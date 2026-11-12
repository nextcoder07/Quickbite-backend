import React, { createContext, useContext, useState, useEffect, useMemo, useCallback } from 'react';
import { Cart } from '../models/Cart';
import { Shop } from '../models/Shop';
import { useDialog } from './DialogContext';

const CartContext = createContext(null);

const CART_STORAGE_KEY = 'qb_cart';
const SHOP_STORAGE_KEY = 'qb_cart_shop';

export const CartProvider = ({ children }) => {
  const dialog = useDialog();
  const [cartState, setCartState] = useState(() => {
    try {
      const savedItems = localStorage.getItem(CART_STORAGE_KEY);
      const savedShop = localStorage.getItem(SHOP_STORAGE_KEY);
      const parsedItems = savedItems ? JSON.parse(savedItems) : [];
      const parsedShop = savedShop ? JSON.parse(savedShop) : null;
      return new Cart(parsedItems, parsedShop);
    } catch {
      return new Cart();
    }
  });

  // Sync to local storage
  useEffect(() => {
    try {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cartState.items.map((i) => i.toJSON())));
      localStorage.setItem(SHOP_STORAGE_KEY, JSON.stringify(cartState.shop ? cartState.shop.toJSON() : null));
    } catch (err) {
      console.warn('Could not persist cart to localStorage', err);
    }
  }, [cartState]);

  const addToCart = useCallback(async (product, targetShop) => {
    const checkCart = new Cart(cartState.items, cartState.shop);
    const check = checkCart.addItem(product, targetShop, false);

    if (check.conflict) {
      const confirmSwitch = await dialog.confirm({
        title: 'Switch Shop in Cart?',
        message: `Your cart contains items from "${check.existingShop.name}". Clear your cart and add items from "${check.newShop.name}" instead?`,
        confirmText: 'Clear & Add Item',
        danger: true,
      });
      if (!confirmSwitch) {
        return false;
      }
      setCartState((prevCart) => {
        const nextCart = new Cart(prevCart.items, prevCart.shop);
        nextCart.addItem(product, targetShop, true);
        return nextCart;
      });
      return true;
    }

    setCartState((prevCart) => {
      const nextCart = new Cart(prevCart.items, prevCart.shop);
      const res = nextCart.addItem(product, targetShop, false);
      return res.conflict ? prevCart : nextCart;
    });
    return true;
  }, [cartState, dialog]);

  const updateQuantity = useCallback((productId, delta) => {
    setCartState((prevCart) => {
      const cart = new Cart(prevCart.items, prevCart.shop);
      cart.updateQuantity(productId, delta);
      return cart;
    });
  }, []);

  const removeFromCart = useCallback((productId) => {
    setCartState((prevCart) => {
      const cart = new Cart(prevCart.items, prevCart.shop);
      cart.removeItem(productId);
      return cart;
    });
  }, []);

  const clearCart = useCallback(() => {
    setCartState(new Cart());
    localStorage.removeItem(CART_STORAGE_KEY);
    localStorage.removeItem(SHOP_STORAGE_KEY);
  }, []);

  // Encapsulated Domain Calculations
  const subtotal = useMemo(() => cartState.getSubtotal(), [cartState]);
  const deliveryFee = useMemo(() => cartState.getDeliveryFee(), [cartState]);
  const totalAmount = useMemo(() => cartState.getTotalAmount(), [cartState]);
  const itemCount = useMemo(() => cartState.getItemCount(), [cartState]);
  const items = useMemo(() => cartState.getItems(), [cartState]);
  const shop = cartState.getShop();

  return (
    <CartContext.Provider
      value={{
        cartModel: cartState,
        cart: items.map((i) => ({ product: i.product, quantity: i.quantity })),
        items,
        shop,
        itemCount,
        subtotal,
        deliveryFee,
        totalAmount,
        addToCart,
        updateQuantity,
        removeFromCart,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

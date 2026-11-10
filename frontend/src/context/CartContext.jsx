import React, { createContext, useContext, useState, useEffect, useMemo, useCallback } from 'react';
import { Cart } from '../models/Cart';
import { Shop } from '../models/Shop';

const CartContext = createContext(null);

const CART_STORAGE_KEY = 'qb_cart';
const SHOP_STORAGE_KEY = 'qb_cart_shop';

export const CartProvider = ({ children }) => {
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

  const addToCart = useCallback((product, targetShop) => {
    let result = false;
    setCartState((prevCart) => {
      // Re-hydrate or clone Cart instance
      const cart = new Cart(prevCart.items, prevCart.shop);
      const addResult = cart.addItem(product, targetShop, false);

      if (addResult.conflict) {
        const confirmSwitch = window.confirm(
          `Your cart contains items from "${addResult.existingShop.name}". Would you like to clear your cart and add items from "${addResult.newShop.name}" instead?`
        );
        if (!confirmSwitch) {
          result = false;
          return prevCart;
        }
        // User confirmed: force reset and add
        cart.addItem(product, targetShop, true);
      }

      result = true;
      return cart;
    });
    return result;
  }, []);

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

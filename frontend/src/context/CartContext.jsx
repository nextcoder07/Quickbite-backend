import React, { createContext, useContext, useState, useEffect } from 'react';

const CartContext = createContext(null);

export const CartProvider = ({ children }) => {
  const [cart, setCart] = useState(() => {
    const saved = localStorage.getItem('qb_cart');
    return saved ? JSON.parse(saved) : [];
  });
  const [shop, setShop] = useState(() => {
    const saved = localStorage.getItem('qb_cart_shop');
    return saved ? JSON.parse(saved) : null;
  });

  useEffect(() => {
    localStorage.setItem('qb_cart', JSON.stringify(cart));
    localStorage.setItem('qb_cart_shop', JSON.stringify(shop));
  }, [cart, shop]);

  const addToCart = (product, targetShop) => {
    if (shop && shop.id !== targetShop.id && cart.length > 0) {
      const confirmSwitch = window.confirm(
        `Your cart contains items from "${shop.name}". Would you like to clear your cart and add items from "${targetShop.name}" instead?`
      );
      if (!confirmSwitch) return false;
      setCart([{ product, quantity: 1 }]);
      setShop(targetShop);
      return true;
    }

    if (!shop || cart.length === 0) {
      setShop(targetShop);
    }

    setCart((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        return prev.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        );
      }
      return [...prev, { product, quantity: 1 }];
    });
    return true;
  };

  const updateQuantity = (productId, delta) => {
    setCart((prev) => {
      return prev
        .map((item) => {
          if (item.product.id === productId) {
            const newQty = item.quantity + delta;
            return newQty > 0 ? { ...item, quantity: newQty } : null;
          }
          return item;
        })
        .filter(Boolean);
    });
  };

  const removeFromCart = (productId) => {
    setCart((prev) => prev.filter((item) => item.product.id !== productId));
  };

  const clearCart = () => {
    setCart([]);
    setShop(null);
    localStorage.removeItem('qb_cart');
    localStorage.removeItem('qb_cart_shop');
  };

  const subtotal = cart.reduce((sum, item) => {
    return sum + Number(item.product.price) * item.quantity;
  }, 0);

  const deliveryFee = subtotal > 0 && subtotal < 300 ? 30 : 0;
  const totalAmount = subtotal + deliveryFee;
  const itemCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <CartContext.Provider
      value={{
        cart,
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

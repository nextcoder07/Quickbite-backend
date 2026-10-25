import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { Plus, Edit2, Trash2, X, Package, AlertCircle } from 'lucide-react';

export const ProductManagement = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Add / Edit Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [stockQuantity, setStockQuantity] = useState('');
  const [imagePath, setImagePath] = useState('');
  const [available, setAvailable] = useState(true);
  const [saving, setSaving] = useState(false);

  const fetchProducts = async () => {
    try {
      const data = await api.get('/marketplace/owner/products');
      setProducts(data);
    } catch (err) {
      setError(err.message || 'Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const openCreateModal = () => {
    setEditingProduct(null);
    setName('');
    setDescription('');
    setPrice('');
    setStockQuantity('');
    setImagePath('');
    setAvailable(true);
    setModalOpen(true);
  };

  const openEditModal = (p) => {
    setEditingProduct(p);
    setName(p.name);
    setDescription(p.description || '');
    setPrice(p.price);
    setStockQuantity(p.stockQuantity);
    setImagePath(p.imagePath || '');
    setAvailable(p.available);
    setModalOpen(true);
  };

  const handleSaveProduct = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    try {
      if (editingProduct) {
        await api.put(`/marketplace/owner/products/${editingProduct.id}`, {
          name: name.trim(),
          description: description.trim(),
          price: Number(price),
          stockQuantity: Number(stockQuantity),
          imagePath: imagePath ? imagePath.trim() : null,
          available,
        });
      } else {
        await api.post('/marketplace/owner/products', {
          name: name.trim(),
          description: description.trim(),
          price: Number(price),
          stockQuantity: Number(stockQuantity),
          imagePath: imagePath ? imagePath.trim() : null,
        });
      }
      setModalOpen(false);
      fetchProducts();
    } catch (err) {
      setError(err.message || 'Failed to save product');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteProduct = async (productId) => {
    if (!window.confirm('Are you sure you want to delete this product?')) return;
    try {
      await api.delete(`/marketplace/owner/products/${productId}`);
      fetchProducts();
    } catch (err) {
      alert(err.message || 'Failed to delete product');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Product Catalog</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Manage menu items, prices, inventory availability, and media
          </p>
        </div>

        <button onClick={openCreateModal} className="btn btn-primary">
          <Plus size={16} /> Add New Item
        </button>
      </div>

      {error && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'var(--danger-bg)', color: 'var(--danger)' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading catalog...</div>
      ) : products.length === 0 ? (
        <div className="glass-panel" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <Package size={48} color="var(--text-faint)" style={{ margin: '0 auto 16px' }} />
          <h3>No Menu Items Added</h3>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginTop: '6px', marginBottom: '20px' }}>
            Start by adding fresh dishes, rolls, or beverages to your shop.
          </p>
          <button onClick={openCreateModal} className="btn btn-primary">
            <Plus size={16} /> Add Your First Product
          </button>
        </div>
      ) : (
        <div className="glass-panel" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'var(--bg-surface)', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                <th style={{ padding: '14px 20px' }}>ITEM</th>
                <th style={{ padding: '14px 20px' }}>PRICE</th>
                <th style={{ padding: '14px 20px' }}>INVENTORY</th>
                <th style={{ padding: '14px 20px' }}>STATUS</th>
                <th style={{ padding: '14px 20px', textAlign: 'right' }}>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                  <td style={{ padding: '14px 20px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                      <img
                        src={p.imagePath || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=100'}
                        alt={p.name}
                        style={{ width: '42px', height: '42px', borderRadius: '8px', objectFit: 'cover' }}
                      />
                      <div>
                        <div style={{ fontWeight: 600 }}>{p.name}</div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>{p.description || 'No description'}</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: '14px 20px', fontWeight: 700, color: 'var(--primary)' }}>
                    ₹{Number(p.price).toFixed(2)}
                  </td>
                  <td style={{ padding: '14px 20px', color: p.stockQuantity > 0 ? '#fff' : 'var(--danger)', fontWeight: 600 }}>
                    {p.stockQuantity} units
                  </td>
                  <td style={{ padding: '14px 20px' }}>
                    {p.available && p.stockQuantity > 0 ? (
                      <span className="badge badge-success">Available</span>
                    ) : (
                      <span className="badge badge-danger">Unavailable</span>
                    )}
                  </td>
                  <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '8px' }}>
                      <button onClick={() => openEditModal(p)} className="btn btn-secondary btn-sm" style={{ padding: '6px' }}>
                        <Edit2 size={14} />
                      </button>
                      <button onClick={() => handleDeleteProduct(p.id)} className="btn btn-danger btn-sm" style={{ padding: '6px' }}>
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Product Add / Edit Modal */}
      {modalOpen && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          padding: '20px'
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '480px', padding: '28px', background: '#161922' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h3 style={{ fontSize: '1.25rem' }}>{editingProduct ? 'Edit Menu Item' : 'Add New Menu Item'}</h3>
              <button onClick={() => setModalOpen(false)} style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSaveProduct}>
              <div className="form-group">
                <label className="form-label">Item Name</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Paneer Kathi Roll"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="form-textarea"
                  rows={2}
                  placeholder="Fresh fillings, spices, and mint relish..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div className="form-group">
                  <label className="form-label">Price (₹)</label>
                  <input
                    type="number"
                    step="0.50"
                    min="0"
                    className="form-input"
                    placeholder="e.g. 120.00"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Stock Quantity</label>
                  <input
                    type="number"
                    min="0"
                    className="form-input"
                    placeholder="e.g. 30"
                    value={stockQuantity}
                    onChange={(e) => setStockQuantity(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Image URL</label>
                <input
                  type="url"
                  className="form-input"
                  placeholder="https://images.unsplash.com/..."
                  value={imagePath}
                  onChange={(e) => setImagePath(e.target.value)}
                />
              </div>

              {editingProduct && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: '14px 0' }}>
                  <input
                    type="checkbox"
                    id="availableCheck"
                    checked={available}
                    onChange={(e) => setAvailable(e.target.checked)}
                    style={{ width: '18px', height: '18px', accentColor: 'var(--primary)' }}
                  />
                  <label htmlFor="availableCheck" style={{ fontSize: '0.9rem', cursor: 'pointer' }}>
                    Item is Available for Ordering
                  </label>
                </div>
              )}

              <button
                type="submit"
                disabled={saving}
                className="btn btn-primary"
                style={{ width: '100%', padding: '12px', marginTop: '12px' }}
              >
                {saving ? 'Saving...' : editingProduct ? 'Update Product' : 'Add Product'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

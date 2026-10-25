import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { StatusBadge } from '../../components/StatusBadge';
import { Sliders, AlertTriangle, CheckCircle2, Clock, Upload } from 'lucide-react';

export const ShopSettings = () => {
  const [shop, setShop] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [campusLocation, setCampusLocation] = useState('');
  const [imagePath, setImagePath] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.uploadFile('/storage/upload?folder=shops', formData);
      if (res?.url) {
        setImagePath(res.url);
      }
    } catch (err) {
      alert('Upload to Supabase failed: ' + (err.message || 'Please check your connection'));
    } finally {
      setUploading(false);
    }
  };

  // Deletion Request
  const [deletionRequests, setDeletionRequests] = useState([]);
  const [deletionReason, setDeletionReason] = useState('');
  const [submittingDeletion, setSubmittingDeletion] = useState(false);

  const fetchShop = async () => {
    try {
      const data = await api.get('/marketplace/owner/shop');
      setShop(data);
      setName(data.name);
      setDescription(data.description || '');
      setCampusLocation(data.campusLocation);
      setImagePath(data.imagePath || '');

      const requests = await api.get('/account/deletion-request');
      setDeletionRequests(requests);
    } catch (err) {
      setError(err.message || 'Failed to load shop settings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShop();
  }, []);

  const handleUpdate = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');

    try {
      const updated = await api.put('/marketplace/owner/shop', {
        name: name.trim(),
        description: description.trim(),
        campusLocation: campusLocation.trim(),
        imagePath: imagePath ? imagePath.trim() : null,
      });
      setShop(updated);
      setSuccess('Shop details updated successfully');
    } catch (err) {
      setError(err.message || 'Failed to update shop');
    } finally {
      setSaving(false);
    }
  };

  const handleSubmitDeletion = async (e) => {
    e.preventDefault();
    if (!deletionReason) return;
    setSubmittingDeletion(true);

    try {
      await api.post('/account/deletion-request', {
        reason: deletionReason.trim(),
      });
      setDeletionReason('');
      alert('Account deletion request submitted for administrative review.');
      fetchShop();
    } catch (err) {
      alert(err.message || 'Failed to submit deletion request');
    } finally {
      setSubmittingDeletion(false);
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading settings...</div>;
  }

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>Shop Profile & Settings</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Configure shop identity, location, and account management
        </p>
      </div>

      {success && (
        <div style={{ padding: '10px 14px', borderRadius: '8px', background: 'var(--success-bg)', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle2 size={16} /> <span>{success}</span>
        </div>
      )}

      {error && (
        <div style={{ padding: '10px 14px', borderRadius: '8px', background: 'var(--danger-bg)', color: 'var(--danger)' }}>
          {error}
        </div>
      )}

      {/* Edit Shop Form */}
      <div className="glass-panel" style={{ padding: '28px' }}>
        <form onSubmit={handleUpdate}>
          <div className="form-group">
            <label className="form-label">Shop Name</label>
            <input
              type="text"
              className="form-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Description / Bio</label>
            <textarea
              className="form-textarea"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Campus Location</label>
            <input
              type="text"
              className="form-input"
              value={campusLocation}
              onChange={(e) => setCampusLocation(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
              <label className="form-label" style={{ marginBottom: 0 }}>Cover Image</label>
              <label className="btn btn-outline" style={{ cursor: 'pointer', padding: '4px 10px', fontSize: '12px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                <Upload size={13} />
                {uploading ? 'Uploading...' : 'Upload to Supabase'}
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  style={{ display: 'none' }}
                  disabled={uploading}
                  onChange={handleFileUpload}
                />
              </label>
            </div>
            <input
              type="url"
              className="form-input"
              value={imagePath}
              placeholder="https://... or uploaded image URL"
              onChange={(e) => setImagePath(e.target.value)}
            />
            {imagePath && (
              <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                <img
                  src={imagePath}
                  alt="Shop Preview"
                  style={{ width: '80px', height: '50px', objectFit: 'cover', borderRadius: '6px', border: '1px solid var(--border)' }}
                  onError={(e) => { e.target.style.display = 'none'; }}
                />
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Cover image ready</span>
              </div>
            )}
          </div>

          <button type="submit" disabled={saving} className="btn btn-primary" style={{ marginTop: '8px' }}>
            {saving ? 'Saving...' : 'Save Shop Details'}
          </button>
        </form>
      </div>

      {/* Account Deletion Request Card */}
      <div className="glass-panel" style={{ padding: '24px 28px', border: '1px solid rgba(239, 68, 68, 0.25)', background: 'rgba(239, 68, 68, 0.04)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#f87171', marginBottom: '8px' }}>
          <AlertTriangle size={20} />
          <h3 style={{ fontSize: '1.15rem' }}>Request Shop Owner Account Deletion</h3>
        </div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', lineHeight: '1.5', marginBottom: '16px' }}>
          Under college policy, Shop Owner deletion requires campus admin confirmation to ensure fulfilled orders and settled accounts. Historical records will be retained.
        </p>

        {deletionRequests.length > 0 ? (
          <div style={{ padding: '14px', background: 'var(--bg-surface)', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '6px' }}>CURRENT DELETION REQUEST:</div>
            {deletionRequests.map((req) => (
              <div key={req.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>Reason: {req.reason}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-faint)' }}>Requested at {new Date(req.requestedAt).toLocaleDateString()}</div>
                </div>
                <StatusBadge status={req.status} />
              </div>
            ))}
          </div>
        ) : (
          <form onSubmit={handleSubmitDeletion}>
            <div className="form-group">
              <label className="form-label">Reason for Requesting Account Closure</label>
              <textarea
                className="form-textarea"
                rows={2}
                placeholder="e.g. Graduating campus, shop contract ended..."
                value={deletionReason}
                onChange={(e) => setDeletionReason(e.target.value)}
                required
              />
            </div>
            <button type="submit" disabled={submittingDeletion} className="btn btn-danger btn-sm">
              {submittingDeletion ? 'Submitting...' : 'Submit Deletion Request'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

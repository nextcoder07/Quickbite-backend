import React, { useState } from 'react';
import { MapPin, Navigation, ExternalLink, X } from 'lucide-react';

/**
 * Reusable LocationPicker component supporting freeform addresses anywhere
 * plus optional exact latitude/longitude GPS pinning ("location point").
 */
export const LocationPicker = ({
  label = 'Location / Full Address',
  placeholder = 'Street address, neighborhood, city, landmark...',
  address = '',
  latitude = null,
  longitude = null,
  onChange,
  required = false,
  hint = 'Enter any street address or city, and optionally pin exact GPS coordinates.',
  showCoordinates = true,
}) => {
  const [locating, setLocating] = useState(false);
  const [geoError, setGeoError] = useState('');
  const [showCoords, setShowCoords] = useState(
    Boolean(latitude !== null && latitude !== '' && longitude !== null && longitude !== '')
  );

  const emitChange = (nextAddress, nextLat, nextLng) => {
    onChange?.({
      address: nextAddress,
      latitude: nextLat === '' || nextLat === undefined ? null : nextLat,
      longitude: nextLng === '' || nextLng === undefined ? null : nextLng,
    });
  };

  const handleUseCurrentLocation = () => {
    setGeoError('');
    if (!navigator.geolocation) {
      setGeoError('Browser geolocation is unavailable on this device.');
      return;
    }

    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = Number(pos.coords.latitude.toFixed(6));
        const lng = Number(pos.coords.longitude.toFixed(6));
        const nextAddress = address?.trim() ? address : `Pinned GPS Location (${lat}, ${lng})`;
        setShowCoords(true);
        emitChange(nextAddress, lat, lng);
        setLocating(false);
      },
      (err) => {
        setGeoError(err.message || 'Unable to detect GPS coordinates.');
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 8000 }
    );
  };

  const hasValidCoords =
    latitude !== null &&
    latitude !== '' &&
    longitude !== null &&
    longitude !== '' &&
    Number.isFinite(Number(latitude)) &&
    Number.isFinite(Number(longitude));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
        <label className="form-label" style={{ marginBottom: 0 }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <MapPin size={14} color="var(--brand-primary)" />
            {label}
          </span>
        </label>

        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className="btn btn-secondary"
            style={{ padding: '4px 10px', fontSize: '0.75rem' }}
            onClick={handleUseCurrentLocation}
            disabled={locating}
          >
            <Navigation size={12} />
            {locating ? 'Locating...' : 'Use GPS Point'}
          </button>

          {showCoordinates && (
            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '4px 10px', fontSize: '0.75rem' }}
              onClick={() => setShowCoords((prev) => !prev)}
            >
              {showCoords ? 'Hide Lat/Lng' : 'Set Lat/Lng'}
            </button>
          )}
        </div>
      </div>

      <input
        type="text"
        className="form-input"
        placeholder={placeholder}
        value={address || ''}
        onChange={(e) => emitChange(e.target.value, latitude, longitude)}
        required={required}
      />

      {showCoordinates && showCoords && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr auto',
            gap: '8px',
            alignItems: 'center',
            padding: '10px',
            borderRadius: '10px',
            backgroundColor: 'rgba(15, 23, 42, 0.45)',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <input
            type="number"
            step="0.000001"
            className="form-input"
            placeholder="Latitude (e.g. 12.9716)"
            value={latitude ?? ''}
            onChange={(e) =>
              emitChange(address, e.target.value === '' ? null : Number(e.target.value), longitude)
            }
          />
          <input
            type="number"
            step="0.000001"
            className="form-input"
            placeholder="Longitude (e.g. 77.5946)"
            value={longitude ?? ''}
            onChange={(e) =>
              emitChange(address, latitude, e.target.value === '' ? null : Number(e.target.value))
            }
          />
          {hasValidCoords && (
            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '8px 10px' }}
              title="Clear coordinates"
              onClick={() => emitChange(address, null, null)}
            >
              <X size={14} />
            </button>
          )}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
        {hint && <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{hint}</span>}
        {hasValidCoords && (
          <a
            href={`https://www.google.com/maps?q=${latitude},${longitude}`}
            target="_blank"
            rel="noreferrer"
            style={{ fontSize: '0.75rem', color: 'var(--brand-primary)', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
          >
            <ExternalLink size={12} /> View Point ({Number(latitude).toFixed(4)}, {Number(longitude).toFixed(4)})
          </a>
        )}
      </div>

      {geoError && (
        <div style={{ fontSize: '0.78rem', color: 'var(--status-danger)' }}>{geoError}</div>
      )}
    </div>
  );
};


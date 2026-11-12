import React from 'react';
import { Sparkles } from 'lucide-react';

const PRESET_INTERESTS = [
  'Paneer',
  'Biryani',
  'Dosa',
  'Pizza',
  'Pasta',
  'Burger',
  'Rolls',
  'Coffee',
  'Healthy',
  'Desserts',
];

function parseInterestList(raw) {
  return String(raw || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

/**
 * Reusable comma-separated taste/cuisine preference picker for buyers.
 */
export const FoodInterestSelector = ({
  label = 'Food Interests & Cravings',
  value,
  onChange,
  hint = 'Pick tags or type dishes/cuisines separated by commas. Leave empty for automated top-rated cold-start picks.',
}) => {
  const selected = parseInterestList(value);
  const selectedLower = new Set(selected.map((tag) => tag.toLowerCase()));

  const toggleTag = (tag) => {
    const next = selectedLower.has(tag.toLowerCase())
      ? selected.filter((item) => item.toLowerCase() !== tag.toLowerCase())
      : [...selected, tag];
    onChange(next.join(', '));
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
        <Sparkles size={14} color="#ff6433" /> {label}
      </label>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
        {PRESET_INTERESTS.map((tag) => {
          const active = selectedLower.has(tag.toLowerCase());
          return (
            <button
              key={tag}
              type="button"
              onClick={() => toggleTag(tag)}
              className={`btn btn-sm ${active ? 'btn-primary' : 'btn-secondary'}`}
              style={{
                borderRadius: '999px',
                padding: '5px 12px',
                fontSize: '0.78rem',
              }}
            >
              {active ? `✓ ${tag}` : `+ ${tag}`}
            </button>
          );
        })}
      </div>

      <input
        type="text"
        className="form-input"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="e.g. Paneer, Biryani, Dosa, Thin Crust Pizza"
      />

      {hint && (
        <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
          {hint}
        </div>
      )}
    </div>
  );
};


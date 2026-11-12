import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

/**
 * Reusable password input with an accessible Show/Hide toggle button.
 */
export const PasswordInput = ({
  value,
  onChange,
  placeholder = 'Enter password',
  required = false,
  minLength,
  style,
  inputStyle,
  autoComplete,
  name,
  id,
}) => {
  const [visible, setVisible] = useState(false);

  return (
    <div style={{ position: 'relative', width: '100%', ...style }}>
      <input
        id={id}
        name={name}
        type={visible ? 'text' : 'password'}
        className="form-input"
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
        minLength={minLength}
        autoComplete={autoComplete}
        style={{ paddingRight: '42px', ...inputStyle }}
      />
      <button
        type="button"
        onClick={() => setVisible((prev) => !prev)}
        aria-label={visible ? 'Hide password' : 'Show password'}
        title={visible ? 'Hide password' : 'Show password'}
        style={{
          position: 'absolute',
          right: '10px',
          top: '50%',
          transform: 'translateY(-50%)',
          background: 'transparent',
          border: 'none',
          padding: '4px',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'var(--text-muted)',
          cursor: 'pointer',
        }}
      >
        {visible ? <EyeOff size={16} /> : <Eye size={16} />}
      </button>
    </div>
  );
};


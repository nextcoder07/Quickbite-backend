import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { AlertCircle, CheckCircle2, HelpCircle, X } from 'lucide-react';

const DialogContext = createContext(null);

export const DialogProvider = ({ children }) => {
  const [activeDialog, setActiveDialog] = useState(null);
  const [inputValue, setInputValue] = useState('');
  const [validationError, setValidationError] = useState('');
  const inputRef = useRef(null);

  const openDialog = useCallback((config) => {
    return new Promise((resolve) => {
      setValidationError('');
      setInputValue(config.defaultValue ?? '');
      setActiveDialog({ ...config, resolve });
    });
  }, []);

  const closeWith = useCallback((result) => {
    setActiveDialog((prev) => {
      if (prev?.resolve) {
        prev.resolve(result);
      }
      return null;
    });
    setValidationError('');
  }, []);

  useEffect(() => {
    if (!activeDialog) return;
    if (activeDialog.mode === 'prompt') {
      const timer = setTimeout(() => {
        inputRef.current?.focus();
        inputRef.current?.select();
      }, 15);
      return () => clearTimeout(timer);
    }
  }, [activeDialog]);

  useEffect(() => {
    if (!activeDialog) return;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        if (activeDialog.mode === 'confirm') closeWith(false);
        else if (activeDialog.mode === 'prompt') closeWith(null);
        else closeWith(undefined);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [activeDialog, closeWith]);

  const alert = useCallback(
    (options) => {
      const opts = typeof options === 'string' ? { message: options } : (options || {});
      return openDialog({
        mode: 'alert',
        title: opts.title || (opts.tone === 'success' ? 'Done' : 'Notice'),
        message: opts.message || '',
        confirmText: opts.confirmText || 'OK',
        tone: opts.tone || 'error',
      });
    },
    [openDialog]
  );

  const confirm = useCallback(
    (options) => {
      const opts = typeof options === 'string' ? { message: options } : (options || {});
      return openDialog({
        mode: 'confirm',
        title: opts.title || 'Please Confirm',
        message: opts.message || '',
        confirmText: opts.confirmText || 'Confirm',
        cancelText: opts.cancelText || 'Cancel',
        danger: Boolean(opts.danger),
      });
    },
    [openDialog]
  );

  const prompt = useCallback(
    (options) => {
      const opts = typeof options === 'string' ? { message: options } : (options || {});
      return openDialog({
        mode: 'prompt',
        title: opts.title || 'Input Required',
        message: opts.message || '',
        label: opts.label || '',
        placeholder: opts.placeholder || 'Enter details...',
        defaultValue: opts.defaultValue ?? '',
        required: Boolean(opts.required),
        confirmText: opts.confirmText || 'Submit',
        cancelText: opts.cancelText || 'Cancel',
        danger: Boolean(opts.danger),
      });
    },
    [openDialog]
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!activeDialog) return;
    if (activeDialog.mode === 'prompt') {
      const trimmed = inputValue.trim();
      if (activeDialog.required && !trimmed) {
        setValidationError('Please enter a response before continuing.');
        return;
      }
      closeWith(inputValue);
      return;
    }
    if (activeDialog.mode === 'confirm') {
      closeWith(true);
      return;
    }
    closeWith(undefined);
  };

  const renderIcon = () => {
    if (!activeDialog) return null;
    if (activeDialog.mode === 'alert') {
      if (activeDialog.tone === 'success') {
        return <CheckCircle2 size={20} color="#34d399" style={{ flexShrink: 0 }} />;
      }
      return <AlertCircle size={20} color="var(--danger, #f87171)" style={{ flexShrink: 0 }} />;
    }
    if (activeDialog.danger) {
      return <AlertCircle size={20} color="var(--danger, #f87171)" style={{ flexShrink: 0 }} />;
    }
    return <HelpCircle size={20} color="var(--brand-primary, #ff5722)" style={{ flexShrink: 0 }} />;
  };

  return (
    <DialogContext.Provider value={{ alert, confirm, prompt }}>
      {children}

      {activeDialog && (
        <div
          role="dialog"
          aria-modal="true"
          onClick={() => {
            if (activeDialog.mode === 'confirm') closeWith(false);
            else if (activeDialog.mode === 'prompt') closeWith(null);
            else closeWith(undefined);
          }}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(5, 7, 12, 0.76)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            padding: '20px',
          }}
        >
          <form
            className="glass-panel"
            onSubmit={handleSubmit}
            onClick={(e) => e.stopPropagation()}
            style={{
              width: '100%',
              maxWidth: '460px',
              padding: '24px',
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
              boxShadow: '0 24px 60px rgba(0, 0, 0, 0.55)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                {renderIcon()}
                <h3 style={{ margin: 0, fontSize: '1.1rem' }}>{activeDialog.title}</h3>
              </div>
              <button
                type="button"
                onClick={() => {
                  if (activeDialog.mode === 'confirm') closeWith(false);
                  else if (activeDialog.mode === 'prompt') closeWith(null);
                  else closeWith(undefined);
                }}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: 'var(--text-muted)',
                  cursor: 'pointer',
                  padding: '2px',
                  display: 'flex',
                }}
                aria-label="Close dialog"
              >
                <X size={18} />
              </button>
            </div>

            {activeDialog.message && (
              <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.55 }}>
                {activeDialog.message}
              </p>
            )}

            {activeDialog.mode === 'prompt' && (
              <div className="form-group" style={{ marginBottom: 0 }}>
                {activeDialog.label && <label className="form-label">{activeDialog.label}</label>}
                <input
                  ref={inputRef}
                  type="text"
                  className="form-input"
                  placeholder={activeDialog.placeholder}
                  value={inputValue}
                  onChange={(e) => {
                    setInputValue(e.target.value);
                    if (validationError) setValidationError('');
                  }}
                />
                {validationError && (
                  <div style={{ marginTop: '6px', fontSize: '0.78rem', color: 'var(--danger, #f87171)' }}>
                    {validationError}
                  </div>
                )}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '4px' }}>
              {activeDialog.mode !== 'alert' && (
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => closeWith(activeDialog.mode === 'confirm' ? false : null)}
                >
                  {activeDialog.cancelText}
                </button>
              )}
              <button
                type="submit"
                className={activeDialog.danger ? 'btn btn-danger' : 'btn btn-primary'}
                autoFocus={activeDialog.mode !== 'prompt'}
              >
                {activeDialog.confirmText}
              </button>
            </div>
          </form>
        </div>
      )}
    </DialogContext.Provider>
  );
};

export const useDialog = () => {
  const context = useContext(DialogContext);
  if (!context) {
    throw new Error('useDialog must be used within a DialogProvider');
  }
  return context;
};


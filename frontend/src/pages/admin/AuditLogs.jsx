import React, { useState, useEffect } from 'react';
import { api } from '../../api/apiClient';
import { FileText, Shield, Clock } from 'lucide-react';

export const AuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        const data = await api.get('/admin/audit-logs');
        setLogs(data);
      } catch (err) {
        console.error('Failed to load audit logs:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchLogs();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 style={{ fontSize: '1.8rem', marginBottom: '4px' }}>System Audit Trail</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Append-only security and operational audit logs for compliance and accountability
        </p>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>Loading audit trail...</div>
      ) : logs.length === 0 ? (
        <div className="glass-panel" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No audit logs recorded.
        </div>
      ) : (
        <div className="glass-panel" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'var(--bg-surface)', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                <th style={{ padding: '14px 20px' }}>TIMESTAMP</th>
                <th style={{ padding: '14px 20px' }}>ACTOR</th>
                <th style={{ padding: '14px 20px' }}>ACTION</th>
                <th style={{ padding: '14px 20px' }}>TARGET</th>
                <th style={{ padding: '14px 20px' }}>METADATA</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                  <td style={{ padding: '14px 20px', fontSize: '0.8rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                  <td style={{ padding: '14px 20px', fontWeight: 600, fontSize: '0.85rem' }}>
                    {log.actorEmail}
                  </td>
                  <td style={{ padding: '14px 20px' }}>
                    <span className="badge badge-purple" style={{ fontSize: '0.7rem' }}>
                      {log.action}
                    </span>
                  </td>
                  <td style={{ padding: '14px 20px', fontSize: '0.82rem' }}>
                    {log.targetType} #{log.targetId}
                  </td>
                  <td style={{ padding: '14px 20px', fontSize: '0.78rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                    {log.safeMetadata || '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

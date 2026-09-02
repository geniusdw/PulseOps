import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async fetcher on mount and then every `intervalMs`. Returns
 * { data, error, loading, refresh }. Used by the dashboard and lists so the UI
 * reflects the pipeline in near real time without a websocket.
 */
export function usePolling(fetcher, intervalMs = 4000, deps = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const savedFetcher = useRef(fetcher);
  savedFetcher.current = fetcher;

  const run = useCallback(async () => {
    try {
      const result = await savedFetcher.current();
      setData(result);
      setError(null);
    } catch (e) {
      setError(e?.response?.data?.message || e.message || 'Request failed');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    setLoading(true);
    run();
    if (!intervalMs) return undefined;
    const id = setInterval(run, intervalMs);
    return () => clearInterval(id);
  }, [run, intervalMs]);

  return { data, error, loading, refresh: run };
}

import { Component, type ErrorInfo, type ReactNode } from 'react';
import { TriangleAlert } from 'lucide-react';
import { Button } from './Button';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled render error', error, info.componentStack);
  }

  render() {
    if (!this.state.error) {
      return this.props.children;
    }

    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-canvas px-6 text-center">
        <TriangleAlert className="size-10 text-red-500/70" />
        <div>
          <h1 className="text-lg font-semibold text-slate-100">Something went wrong</h1>
          <p className="mt-1 max-w-md text-sm text-slate-500">
            This page hit an unexpected error. You can try reloading — your data on the server is unaffected.
          </p>
        </div>
        <pre className="max-w-lg overflow-x-auto rounded-lg bg-surface px-4 py-3 text-left text-xs text-red-400">
          {this.state.error.message}
        </pre>
        <Button variant="primary" onClick={() => window.location.reload()}>
          Reload
        </Button>
      </div>
    );
  }
}

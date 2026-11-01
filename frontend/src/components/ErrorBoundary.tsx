import { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  failed: boolean;
}

// Catches errors thrown while DRAWING. It does not catch errors in click handlers,
// timers or async code (those are handled with try/catch where they happen).
export default class ErrorBoundary extends Component<Props, State> {
  state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("The screen crashed:", error, info.componentStack);
  }

  render() {
    if (this.state.failed) {
      return (
        <div className="card">
          <h1>Something went wrong</h1>
          <p>The page hit an unexpected problem. Reloading usually fixes it.</p>
          <button type="button" onClick={() => window.location.reload()}>Reload the page</button>
        </div>
      );
    }
    return this.props.children;
  }
}
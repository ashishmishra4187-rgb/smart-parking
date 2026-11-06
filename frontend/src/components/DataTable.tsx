import type { ReactNode } from "react";
import { EmptyBox } from "./Status";

export interface Column<T> {
  header: string;
  render: (row: T) => ReactNode;
}

interface Props<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  empty?: string;
}

// <T> means "any kind of row": one component for sessions, reservations and detections
export default function DataTable<T>({ columns, rows, rowKey, empty = "Nothing to show." }: Props<T>) {
  if (rows.length === 0) return <EmptyBox>{empty}</EmptyBox>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((c) => <th key={c.header}>{c.header}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((c) => <td key={c.header}>{c.render(row)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
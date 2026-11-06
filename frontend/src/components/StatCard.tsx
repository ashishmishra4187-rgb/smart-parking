interface Props {
  label: string;
  value: number | string;
  hint?: string;
  tone?: "normal" | "good" | "warn";
}

export default function StatCard({ label, value, hint, tone = "normal" }: Props) {
  return (
    <div className={`stat stat-${tone}`}>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}
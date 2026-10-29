import { STATUS_LABEL, STATUS_ORDER } from "../slotInfo";

export default function Legend() {
  return (
    <>
      <div className="legend">
        {STATUS_ORDER.map((st) => (
          <span key={st} className={`badge status-${st}`}>
            {STATUS_LABEL[st]} = {st}
          </span>
        ))}
      </div>
      <p className="tags-help">
        Tags: ACC = disabled access, EV = electric vehicle, BIKE = motorbike, VIS = visitor, STAFF = staff.
      </p>
    </>
  );
}
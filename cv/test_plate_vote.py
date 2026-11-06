import pytest

import gate_client
from gate_client import GateBusy, GateError, send_with_retry
from plate_vote import Cooldown, PlateVoter

A = "DL01AB1234"
B = "MH12DE1433"


def test_one_reading_is_not_enough():
    assert PlateVoter().add(A, 0.9) is None


def test_two_of_three_agree():
    voter = PlateVoter()
    assert voter.add(A, 0.90) is None
    plate, conf = voter.add(A, 0.80)
    assert plate == A
    assert conf == pytest.approx(0.85)


def test_a_different_plate_in_between_does_not_stop_agreement():
    voter = PlateVoter()
    assert voter.add(A, 0.9) is None
    assert voter.add(B, 0.9) is None
    assert voter.add(A, 0.9)[0] == A


def test_low_confidence_frames_do_not_count():
    voter = PlateVoter(min_confidence=0.6)
    assert voter.add(A, 0.30) is None
    assert voter.add(A, 0.30) is None
    assert voter.add(A, 0.30) is None


def test_unreadable_frames_push_old_votes_out():
    voter = PlateVoter()
    voter.add(A, 0.9)
    voter.add(None, 0.0)
    voter.add(None, 0.0)
    assert voter.add(A, 0.9) is None          # the first A has already left the window of 3


def test_agreement_is_reported_only_once():
    voter = PlateVoter()
    voter.add(A, 0.9)
    assert voter.add(A, 0.9) is not None
    assert voter.add(A, 0.9) is None           # the window was cleared
    assert voter.add(A, 0.9) is not None


def test_cooldown():
    cd = Cooldown()
    assert cd.ready(A, 100.0)
    cd.mark(A, 100.0, 20.0)
    assert not cd.ready(A, 110.0)
    assert cd.ready(A, 120.0)
    assert cd.ready(B, 110.0)                  # other cars are not affected


def test_retry_until_success(monkeypatch):
    calls = {"n": 0}

    def fake(plate, confidence, direction):
        calls["n"] += 1
        if calls["n"] < 3:
            raise GateBusy("down")
        return {"decision": "APPROVED"}

    monkeypatch.setattr(gate_client, "send_detection", fake)
    waits = []
    result = send_with_retry(A, 0.9, "entry", attempts=3, delay=1.0, sleep=waits.append)
    assert result["decision"] == "APPROVED"
    assert waits == [1.0, 2.0]


def test_retry_gives_up(monkeypatch):
    def fake(plate, confidence, direction):
        raise GateBusy("down")

    monkeypatch.setattr(gate_client, "send_detection", fake)
    waits = []
    with pytest.raises(GateBusy):
        send_with_retry(A, 0.9, "entry", attempts=2, delay=1.0, sleep=waits.append)
    assert waits == [1.0]


def test_permanent_errors_are_not_retried(monkeypatch):
    calls = {"n": 0}

    def fake(plate, confidence, direction):
        calls["n"] += 1
        raise GateError("wrong key")

    monkeypatch.setattr(gate_client, "send_detection", fake)
    with pytest.raises(GateError):
        send_with_retry(A, 0.9, "entry", attempts=3, sleep=lambda s: None)
    assert calls["n"] == 1
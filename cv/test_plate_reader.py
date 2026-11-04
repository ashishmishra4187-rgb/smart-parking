from plate_reader import clean_text, fix_by_position


def test_clean_text():
    assert clean_text("dl 01-ab 1234") == "DL01AB1234"


def test_already_correct():
    assert fix_by_position("DL01AB1234") == ("DL01AB1234", 0)


def test_letter_o_in_a_digit_position():
    assert fix_by_position("DLO1AB1234") == ("DL01AB1234", 1)


def test_digit_in_a_letter_position():
    assert fix_by_position("DL01A81234") == ("DL01AB1234", 1)


def test_too_many_fixes_is_refused():
    assert fix_by_position("D1O1A81Z34") == (None, 0)


def test_wrong_length_and_garbage():
    assert fix_by_position("DL01AB12") == (None, 0)
    assert fix_by_position("HELLO") == (None, 0)
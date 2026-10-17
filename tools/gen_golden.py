#!/usr/bin/env python3
"""Regenerates app/src/test/resources/golden/*.csv from the independent reference library `pyluach` (pip install pyluach==2.3.0).

The Kotlin calendar code is compared against these files by GoldenCalendarTest, so a mistake in the Hebrew calendar,
parasha or holiday logic shows up in CI. Run:  python3 tools/gen_golden.py
"""
import pathlib
from datetime import date, timedelta
from pyluach import dates, parshios

OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/test/resources/golden"
OUT.mkdir(parents=True, exist_ok=True)


def g(d):
    return dates.GregorianDate(d.year, d.month, d.day)


# 1) Gregorian -> Hebrew (year, month with Nisan=1 ... Tishrei=7, Adar II=13, day), every 5th day 1950..2100
with open(OUT / "hebrew_dates.csv", "w", encoding="utf-8") as f:
    d = date(1950, 1, 1)
    while d < date(2100, 12, 31):
        h = g(d).to_heb()
        f.write(f"{d.isoformat()},{h.year},{h.month},{h.day}\n")
        d += timedelta(5)

# 2) Weekly parasha for every Shabbat 2000..2100: date,israel(0/1),indices joined by ';' (empty = no regular parasha)
with open(OUT / "parsha.csv", "w", encoding="utf-8") as f:
    d = date(2000, 1, 1)
    while d.weekday() != 5:
        d += timedelta(1)
    while d < date(2100, 1, 1):
        for il in (0, 1):
            p = parshios.getparsha(g(d), israel=bool(il)) or []
            f.write(f"{d.isoformat()},{il},{';'.join(map(str, p))}\n")
        d += timedelta(7)

# 3) Festivals and fasts (family level) for every day 2000..2060
FEST = {
    "Chanuka": "CHANUKAH", "Lag Ba'omer": "LAG_BAOMER", "Pesach": "PESACH", "Pesach Sheni": "PESACH_SHENI", "Purim": "PURIM", "Purim Katan": "PURIM_KATAN",
    "Rosh Hashana": "ROSH_HASHANA", "Shavuos": "SHAVUOT", "Shmini Atzeres": "SHMINI_ATZERET", "Simchas Torah": "SIMCHAT_TORAH",
    "Succos": "SUKKOT", "Tu B'av": "TU_BAV", "Tu B'shvat": "TU_BISHVAT", "Yom Kippur": "YOM_KIPPUR", "Shushan Purim": "SHUSHAN_PURIM",
}
FAST = {
    "Tzom Gedalia": "TZOM_GEDALIAH", "10 of Teves": "TENTH_OF_TEVET", "Taanis Esther": "TAANIT_ESTHER",
    "17 of Tamuz": "SEVENTEENTH_OF_TAMMUZ", "9 of Av": "TISHA_BAV",
}
with open(OUT / "marks.csv", "w", encoding="utf-8") as f:
    d = date(2000, 1, 1)
    while d < date(2060, 1, 1):
        for il in (0, 1):
            kinds = set()
            fe = g(d).festival(israel=bool(il), include_working_days=True)
            if fe:
                kinds.add(FEST[fe])
            fa = g(d).fast_day()
            if fa:
                kinds.add(FAST[fa])
            if kinds:
                f.write(f"{d.isoformat()},{il},{';'.join(sorted(kinds))}\n")
        d += timedelta(1)
print("golden files written to", OUT)

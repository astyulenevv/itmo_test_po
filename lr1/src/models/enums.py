from enum import Enum


class CarModel(Enum):
    X1 = "X1"
    X3 = "X3"
    X5 = "X5"
    SERIES_3 = "3 Series"
    SERIES_5 = "5 Series"
    SERIES_7 = "7 Series"
    M3 = "M3"
    M5 = "M5"


class CarStatus(Enum):
    AVAILABLE = "available"
    SOLD = "sold"
    RESERVED = "reserved"
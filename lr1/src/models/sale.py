from datetime import datetime
from dataclasses import dataclass
from .car import Car
from .customer import Customer
from .enums import CarStatus


@dataclass
class Sale:
    id: str
    car: Car
    customer: Customer
    sale_date: datetime
    final_price: float
    
    def __post_init__(self):
        if self.final_price <= 0:
            raise ValueError("Sale price must be positive")
        if self.car.status != CarStatus.SOLD:
            raise ValueError("Car must be marked as sold")
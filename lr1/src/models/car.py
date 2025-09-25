from datetime import datetime
from dataclasses import dataclass
from .enums import CarModel, CarStatus


@dataclass
class Car:
    id: str
    model: CarModel
    year: int
    price: float
    color: str
    mileage: int = 0
    status: CarStatus = CarStatus.AVAILABLE
    
    def __post_init__(self):
        if self.year < 2000 or self.year > datetime.now().year + 1:
            raise ValueError("Invalid year: must be between 2000 and next year")
        if self.price <= 0:
            raise ValueError("Price must be positive")
        if self.mileage < 0:
            raise ValueError("Mileage cannot be negative")
    
    def apply_discount(self, discount_percent: float) -> float:
        if not 0 <= discount_percent <= 100:
            raise ValueError("Discount must be between 0 and 100 percent")
        
        discount_amount = self.price * (discount_percent / 100)
        return self.price - discount_amount
    
    def calculate_depreciation(self) -> float:
        current_year = datetime.now().year
        age = current_year - self.year
        
        age_depreciation = age * 0.1
        mileage_depreciation = (self.mileage / 10000) * 0.01
        
        total_depreciation = min(age_depreciation + mileage_depreciation, 0.8)
        return self.price * (1 - total_depreciation)
from datetime import datetime
from typing import List, Dict
from ..models import Car, Customer, Sale, CarModel, CarStatus


class BMWDealership:
    def __init__(self, name: str):
        self.name = name
        self.cars: Dict[str, Car] = {}
        self.customers: Dict[str, Customer] = {}
        self.sales: Dict[str, Sale] = {}
    
    def add_car(self, car: Car) -> None:
        if car.id in self.cars:
            raise ValueError(f"Car with ID {car.id} already exists")
        self.cars[car.id] = car
    
    def remove_car(self, car_id: str) -> None:
        if car_id not in self.cars:
            raise ValueError(f"Car with ID {car_id} not found")
        del self.cars[car_id]
    
    def add_customer(self, customer: Customer) -> None:
        if customer.id in self.customers:
            raise ValueError(f"Customer with ID {customer.id} already exists")
        self.customers[customer.id] = customer
    
    def get_available_cars(self) -> List[Car]:
        return [car for car in self.cars.values() if car.status == CarStatus.AVAILABLE]
    
    def get_cars_by_model(self, model: CarModel) -> List[Car]:
        return [car for car in self.cars.values() if car.model == model]
    
    def get_cars_in_budget(self, budget: float) -> List[Car]:
        return [car for car in self.get_available_cars() if car.price <= budget]
    
    def reserve_car(self, car_id: str, customer_id: str) -> None:
        if car_id not in self.cars:
            raise ValueError(f"Car with ID {car_id} not found")
        if customer_id not in self.customers:
            raise ValueError(f"Customer with ID {customer_id} not found")
        
        car = self.cars[car_id]
        if car.status != CarStatus.AVAILABLE:
            raise ValueError(f"Car {car_id} is not available for reservation")
        
        car.status = CarStatus.RESERVED
    
    def sell_car(self, car_id: str, customer_id: str, discount_percent: float = 0) -> Sale:
        if car_id not in self.cars:
            raise ValueError(f"Car with ID {car_id} not found")
        if customer_id not in self.customers:
            raise ValueError(f"Customer with ID {customer_id} not found")
        
        car = self.cars[car_id]
        customer = self.customers[customer_id]
        
        if car.status == CarStatus.SOLD:
            raise ValueError(f"Car {car_id} is already sold")
        
        final_price = car.apply_discount(discount_percent)
        
        if final_price > customer.budget:
            raise ValueError(f"Customer budget ({customer.budget}) insufficient for car price ({final_price})")
        
        car.status = CarStatus.SOLD
        
        sale_id = f"sale_{len(self.sales) + 1}"
        sale = Sale(
            id=sale_id,
            car=car,
            customer=customer,
            sale_date=datetime.now(),
            final_price=final_price
        )
        
        self.sales[sale_id] = sale
        return sale
    
    def get_total_sales_value(self) -> float:
        return sum(sale.final_price for sale in self.sales.values())
    
    def get_inventory_value(self) -> float:
        return sum(car.price for car in self.get_available_cars())
    
    def get_sales_by_model(self, model: CarModel) -> List[Sale]:
        return [sale for sale in self.sales.values() if sale.car.model == model]
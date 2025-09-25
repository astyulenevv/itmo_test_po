import unittest
from datetime import datetime
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from src.models import Car, Customer, Sale, CarModel, CarStatus
from src.services import BMWDealership


class TestBMWDealership(unittest.TestCase):
    
    def setUp(self):
        self.dealership = BMWDealership("BMW Munich")
        
        self.car1 = Car("BMW001", CarModel.X5, 2022, 65000.0, "Black", 10000)
        self.car2 = Car("BMW002", CarModel.SERIES_3, 2023, 45000.0, "White", 5000)
        self.car3 = Car("BMW003", CarModel.M3, 2021, 75000.0, "Red", 20000)
        
        self.customer1 = Customer("CUST001", "John Doe", "john@email.com", "+123456789", 70000.0)
        self.customer2 = Customer("CUST002", "Jane Smith", "jane@email.com", "+987654321", 40000.0)
    
    def test_dealership_creation(self):
        # Arrange & Act
        dealership = BMWDealership("Test Dealership")
        
        # Assert
        self.assertEqual(dealership.name, "Test Dealership")
        self.assertEqual(len(dealership.cars), 0)
        self.assertEqual(len(dealership.customers), 0)
        self.assertEqual(len(dealership.sales), 0)
    
    def test_add_car_to_inventory(self):
        # Arrange
        initial_count = len(self.dealership.cars)
        
        # Act
        self.dealership.add_car(self.car1)
        
        # Assert
        self.assertEqual(len(self.dealership.cars), initial_count + 1)
        self.assertIn(self.car1.id, self.dealership.cars)
        self.assertEqual(self.dealership.cars[self.car1.id], self.car1)
    
    def test_add_duplicate_car_raises_error(self):
        # Arrange
        self.dealership.add_car(self.car1)
        
        # Act & Assert
        with self.assertRaises(ValueError) as context:
            self.dealership.add_car(self.car1)
        self.assertIn("already exists", str(context.exception))
    
    def test_add_customer(self):
        # Arrange
        initial_count = len(self.dealership.customers)
        
        # Act
        self.dealership.add_customer(self.customer1)
        
        # Assert
        self.assertEqual(len(self.dealership.customers), initial_count + 1)
        self.assertIn(self.customer1.id, self.dealership.customers)
    
    def test_get_available_cars(self):
        # Arrange
        self.dealership.add_car(self.car1)
        self.dealership.add_car(self.car2)
        self.dealership.add_car(self.car3)
        
        self.car3.status = CarStatus.SOLD
        
        # Act
        available_cars = self.dealership.get_available_cars()
        
        # Assert
        self.assertEqual(len(available_cars), 2)
        self.assertIn(self.car1, available_cars)
        self.assertIn(self.car2, available_cars)
        self.assertNotIn(self.car3, available_cars)
    
    def test_get_cars_by_model(self):
        # Arrange
        self.dealership.add_car(self.car1)
        self.dealership.add_car(self.car2)
        self.dealership.add_car(self.car3)
        
        # Act
        x5_cars = self.dealership.get_cars_by_model(CarModel.X5)
        series3_cars = self.dealership.get_cars_by_model(CarModel.SERIES_3)
        
        # Assert
        self.assertEqual(len(x5_cars), 1)
        self.assertEqual(x5_cars[0], self.car1)
        self.assertEqual(len(series3_cars), 1)
        self.assertEqual(series3_cars[0], self.car2)
    
    def test_get_cars_in_budget(self):
        # Arrange
        self.dealership.add_car(self.car1)
        self.dealership.add_car(self.car2)
        self.dealership.add_car(self.car3)
        
        budget = 50000.0
        
        # Act
        affordable_cars = self.dealership.get_cars_in_budget(budget)
        
        # Assert
        self.assertEqual(len(affordable_cars), 1)
        self.assertIn(self.car2, affordable_cars)
        self.assertNotIn(self.car1, affordable_cars)
        self.assertNotIn(self.car3, affordable_cars)
    
    def test_reserve_car(self):
        # Arrange
        self.dealership.add_car(self.car1)
        self.dealership.add_customer(self.customer1)
        
        # Act
        self.dealership.reserve_car(self.car1.id, self.customer1.id)
        
        # Assert
        self.assertEqual(self.car1.status, CarStatus.RESERVED)
    
    def test_reserve_nonexistent_car_raises_error(self):
        # Arrange
        self.dealership.add_customer(self.customer1)
        
        # Act & Assert
        with self.assertRaises(ValueError) as context:
            self.dealership.reserve_car("NONEXISTENT", self.customer1.id)
        self.assertIn("not found", str(context.exception))
    
    def test_remove_car_from_inventory(self):
        # Arrange
        car = Car("BMW001", CarModel.X1, 2022, 35000.0, "Red")
        self.dealership.add_car(car)
        
        # Act
        self.dealership.remove_car(car.id)
        
        # Assert
        self.assertNotIn(car.id, self.dealership.cars)
        self.assertEqual(len(self.dealership.cars), 0)
    
    def test_remove_nonexistent_car_raises_error(self):
        # Arrange & Act & Assert
        with self.assertRaises(ValueError) as context:
            self.dealership.remove_car("NONEXISTENT")
        self.assertIn("not found", str(context.exception))


class TestSalesProcess(unittest.TestCase):
    
    def setUp(self):
        self.dealership = BMWDealership("BMW Sales Center")
        
        self.car = Car("BMW001", CarModel.SERIES_5, 2022, 55000.0, "Blue", 8000)
        self.customer = Customer("CUST001", "Alice Johnson", "alice@email.com", "+111222333", 60000.0)
        
        self.dealership.add_car(self.car)
        self.dealership.add_customer(self.customer)
    
    def test_successful_car_sale(self):
        # Arrange
        discount_percent = 5.0
        expected_final_price = self.car.price * 0.95
        
        # Act
        sale = self.dealership.sell_car(self.car.id, self.customer.id, discount_percent)
        
        # Assert
        self.assertIsInstance(sale, Sale)
        self.assertEqual(sale.car, self.car)
        self.assertEqual(sale.customer, self.customer)
        self.assertEqual(sale.final_price, expected_final_price)
        self.assertEqual(self.car.status, CarStatus.SOLD)
        self.assertIn(sale.id, self.dealership.sales)
    
    def test_sale_exceeds_customer_budget_raises_error(self):
        # Arrange
        expensive_car = Car("BMW002", CarModel.M5, 2023, 90000.0, "Black")
        self.dealership.add_car(expensive_car)
        
        # Act & Assert
        with self.assertRaises(ValueError) as context:
            self.dealership.sell_car(expensive_car.id, self.customer.id, 0)
        self.assertIn("insufficient", str(context.exception))
    
    def test_sell_already_sold_car_raises_error(self):
        # Arrange
        self.car.status = CarStatus.SOLD
        
        # Act & Assert
        with self.assertRaises(ValueError) as context:
            self.dealership.sell_car(self.car.id, self.customer.id)
        self.assertIn("already sold", str(context.exception))
    
    def test_get_total_sales_value(self):
        # Arrange
        car2 = Car("BMW003", CarModel.X1, 2021, 35000.0, "White")
        customer2 = Customer("CUST002", "Bob Davis", "bob@email.com", "+444555666", 40000.0)
        
        self.dealership.add_car(car2)
        self.dealership.add_customer(customer2)
        
        # Act
        sale1 = self.dealership.sell_car(self.car.id, self.customer.id, 0)
        sale2 = self.dealership.sell_car(car2.id, customer2.id, 10)
        
        total_sales_value = self.dealership.get_total_sales_value()
        
        # Assert
        expected_total = sale1.final_price + sale2.final_price
        self.assertEqual(total_sales_value, expected_total)
        self.assertEqual(total_sales_value, 55000.0 + 31500.0)
    
    def test_get_inventory_value(self):
        # Arrange
        car2 = Car("BMW004", CarModel.X3, 2022, 48000.0, "Silver")
        car3 = Car("BMW005", CarModel.SERIES_7, 2023, 85000.0, "Gold")
        
        self.dealership.add_car(car2)
        self.dealership.add_car(car3)
        
        customer2 = Customer("CUST003", "Charlie Brown", "charlie@email.com", "+777888999", 50000.0)
        self.dealership.add_customer(customer2)
        self.dealership.sell_car(car2.id, customer2.id, 0)
        
        # Act
        inventory_value = self.dealership.get_inventory_value()
        
        # Assert
        expected_value = self.car.price + car3.price
        self.assertEqual(inventory_value, expected_value)
    
    def test_get_sales_by_model(self):
        # Arrange
        x5_car = Car("BMW100", CarModel.X5, 2022, 65000.0, "Black")
        series3_car = Car("BMW200", CarModel.SERIES_3, 2023, 45000.0, "White")
        customer1 = Customer("CUST100", "John Doe", "john@email.com", "+123", 70000.0)
        customer2 = Customer("CUST200", "Jane Smith", "jane@email.com", "+456", 50000.0)
        
        self.dealership.add_car(x5_car)
        self.dealership.add_car(series3_car)
        self.dealership.add_customer(customer1)
        self.dealership.add_customer(customer2)
        
        # Act
        x5_sale = self.dealership.sell_car(x5_car.id, customer1.id, 0)
        series3_sale = self.dealership.sell_car(series3_car.id, customer2.id, 0)
        
        x5_sales = self.dealership.get_sales_by_model(CarModel.X5)
        series3_sales = self.dealership.get_sales_by_model(CarModel.SERIES_3)
        
        # Assert
        self.assertEqual(len(x5_sales), 1)
        self.assertEqual(len(series3_sales), 1)
        self.assertIn(x5_sale, x5_sales)
        self.assertIn(series3_sale, series3_sales)


if __name__ == '__main__':
    unittest.main(verbosity=2)
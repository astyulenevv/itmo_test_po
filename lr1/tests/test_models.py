import unittest
from datetime import datetime
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from src.models import Car, Customer, Sale, CarModel, CarStatus


class TestCar(unittest.TestCase):
    
    def setUp(self):
        self.valid_car = Car(
            id="BMW001",
            model=CarModel.X5,
            year=2022,
            price=65000.0,
            color="Black",
            mileage=15000
        )
    
    def test_car_creation_valid_data(self):
        # Arrange
        car_data = {
            "id": "BMW002",
            "model": CarModel.SERIES_3,
            "year": 2023,
            "price": 45000.0,
            "color": "White",
            "mileage": 5000
        }
        
        # Act
        car = Car(**car_data)
        
        # Assert
        self.assertEqual(car.id, "BMW002")
        self.assertEqual(car.model, CarModel.SERIES_3)
        self.assertEqual(car.year, 2023)
        self.assertEqual(car.price, 45000.0)
        self.assertEqual(car.color, "White")
        self.assertEqual(car.mileage, 5000)
        self.assertEqual(car.status, CarStatus.AVAILABLE)
    
    def test_car_invalid_year_raises_error(self):
        # Arrange & Act & Assert
        with self.assertRaises(ValueError) as context:
            Car(
                id="BMW003",
                model=CarModel.X1,
                year=1990,
                price=30000.0,
                color="Blue"
            )
        self.assertIn("Invalid year", str(context.exception))
    
    def test_car_negative_price_raises_error(self):
        # Arrange & Act & Assert
        with self.assertRaises(ValueError) as context:
            Car(
                id="BMW004",
                model=CarModel.M3,
                year=2022,
                price=-1000.0,
                color="Red"
            )
        self.assertIn("Price must be positive", str(context.exception))
    
    def test_apply_discount_valid_percentage(self):
        # Arrange
        discount_percent = 10.0
        expected_price = self.valid_car.price * 0.9
        
        # Act
        discounted_price = self.valid_car.apply_discount(discount_percent)
        
        # Assert
        self.assertEqual(discounted_price, expected_price)
        self.assertEqual(discounted_price, 58500.0)
    
    def test_apply_discount_invalid_percentage_raises_error(self):
        # Arrange & Act & Assert
        with self.assertRaises(ValueError) as context:
            self.valid_car.apply_discount(150.0)
        self.assertIn("Discount must be between 0 and 100 percent", str(context.exception))
    
    def test_calculate_depreciation(self):
        # Arrange
        old_car = Car(
            id="BMW005",
            model=CarModel.SERIES_5,
            year=2018,
            price=50000.0,
            color="Silver",
            mileage=60000
        )
        
        # Act
        depreciated_value = old_car.calculate_depreciation()
        
        # Assert
        self.assertLess(depreciated_value, old_car.price)
        self.assertGreater(depreciated_value, 0)


class TestCustomer(unittest.TestCase):
    
    def test_customer_creation_valid_data(self):
        # Arrange
        customer_data = {
            "id": "CUST001",
            "name": "John Doe",
            "email": "john.doe@email.com",
            "phone": "+1234567890",
            "budget": 50000.0
        }
        
        # Act
        customer = Customer(**customer_data)
        
        # Assert
        self.assertEqual(customer.id, "CUST001")
        self.assertEqual(customer.name, "John Doe")
        self.assertEqual(customer.email, "john.doe@email.com")
        self.assertEqual(customer.phone, "+1234567890")
        self.assertEqual(customer.budget, 50000.0)
    
    def test_customer_empty_name_raises_error(self):
        # Arrange & Act & Assert
        with self.assertRaises(ValueError) as context:
            Customer(
                id="CUST002",
                name="",
                email="test@email.com",
                phone="+1234567890",
                budget=30000.0
            )
        self.assertIn("Customer name cannot be empty", str(context.exception))
    
    def test_customer_invalid_email_raises_error(self):
        # Arrange & Act & Assert
        with self.assertRaises(ValueError) as context:
            Customer(
                id="CUST003",
                name="Jane Smith",
                email="invalid-email",
                phone="+1234567890",
                budget=40000.0
            )
        self.assertIn("Invalid email format", str(context.exception))
    
    def test_customer_negative_budget_raises_error(self):
        # Arrange & Act & Assert
        with self.assertRaises(ValueError) as context:
            Customer(
                id="CUST004",
                name="Bob Wilson",
                email="bob@email.com",
                phone="+1234567890",
                budget=-5000.0
            )
        self.assertIn("Budget cannot be negative", str(context.exception))


if __name__ == '__main__':
    unittest.main(verbosity=2)
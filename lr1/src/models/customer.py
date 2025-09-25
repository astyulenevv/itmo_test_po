from dataclasses import dataclass


@dataclass
class Customer:
    id: str
    name: str
    email: str
    phone: str
    budget: float
    
    def __post_init__(self):
        if not self.name.strip():
            raise ValueError("Customer name cannot be empty")
        if self.budget < 0:
            raise ValueError("Budget cannot be negative")
        if "@" not in self.email:
            raise ValueError("Invalid email format")
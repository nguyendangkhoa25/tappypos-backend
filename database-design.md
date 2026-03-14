# Multi-Tenant Retail Platform – Database Design

This document describes the database design used for a flexible multi-tenant product catalog system. The system supports multiple product types and dynamic product attributes without requiring schema changes when new attributes are introduced.

The design follows an Entity–Attribute–Value (EAV) pattern optimized for relational databases.

## Supported Product Types

The system is designed to support the following default product types:

- Food
- Beverage
- Drug / Pharmacy
- Convenience Item
- Bike / Motorbike
- Hardware / Tools
- Clothing / Apparel
- Electronics
- Furniture
- Beauty / Personal Care
- Toys / Games
- Books / Media
- Sports / Outdoor
- Automotive Parts
- Home Appliances
- Office Supplies
- Pet Supplies
- Health / Wellness

Each product type can define its own attributes while sharing the same core product table.

---

# Architecture Overview

Core tables:

ProductType
AttributeGroup
AttributeDefinition
Product
ProductAttributeValue
Category
ProductCategory

Purpose of each table:

| Table | Purpose |
|------|--------|
| ProductType | Defines available product types |
| AttributeGroup | Logical grouping of attributes for UI display |
| AttributeDefinition | Defines attributes allowed for a product type |
| Product | Base product information |
| ProductAttributeValue | Stores attribute values for each product |
| Category | Product categories |
| ProductCategory | Many-to-many relationship between products and categories |

---

# Table Definitions

## ProductType

Defines supported product types.

Fields:

- id (PK)
- code
- name
- description
- created_at

Example records:

FOOD
BEVERAGE
DRUG
CONVENIENCE
BIKE

---

## AttributeGroup

Groups attributes for better UI organization.

Examples:

Food:

- Nutrition
- Food Details

Drug:

- Medical Info

Bike:

- Engine
- Technical Specification

Fields:

- id
- product_type_id
- name
- display_order

---

## AttributeDefinition

Defines which attributes belong to a product type.

Fields:

- id
- product_type_id
- attribute_group_id
- code
- name
- data_type
- required
- searchable
- filterable
- display_order
- created_at

Supported data types:

STRING
TEXT
NUMBER
BOOLEAN
DATE

Example attributes:

Food

- expiry_date
- calories
- ingredients
- weight

Beverage

- volume
- sugar_level
- carbonated

Drug

- dosage
- manufacturer
- prescription_required
- active_ingredient

Convenience Item

- brand
- qrcode
- material

Bike / Motorbike

- engine_capacity
- fuel_type
- transmission
- max_speed

---

## Product

Stores common product information shared by all product types.

Fields:

- id
- product_type_id
- sku
- name
- description
- price
- status
- created_at
- updated_at

Only common fields are stored here. Product-specific attributes are stored separately.

---

## ProductAttributeValue

Stores attribute values for each product.

Fields:

- id
- product_id
- attribute_id
- value_string
- value_number
- value_boolean
- value_date
- created_at

Only one value column should be used depending on the attribute data type.

Indexes:

- (product_id, attribute_id)

Constraint:

Unique(product_id, attribute_id)

---

## Category

Defines hierarchical product categories.

Fields:

- id
- name
- parent_id
- created_at

---

## ProductCategory

Join table between products and categories.

Fields:

- product_id
- category_id

Primary key:

(product_id, category_id)

---

# Product Creation Flow

1. Client selects product type
2. System loads attribute definitions for that type
3. UI dynamically generates the product form
4. User fills in product attributes
5. System saves:

- Product record
- ProductAttributeValue records

---

# Attribute Loading Example

When product type = BIKE

System query:

SELECT * FROM attribute_definition WHERE product_type_id = ?

Returned attributes:

- engine_capacity
- fuel_type
- transmission
- max_speed

The frontend dynamically renders the input form based on these attributes.

---

# Advantages of This Design

Flexible

New product types and attributes can be added without modifying the database schema.

Scalable

Supports large catalogs with many attribute variations.

Extensible

New industries or product categories can be added easily.

UI Friendly

Attributes are grouped for dynamic form rendering.

---

# Recommended Indexes

Product

INDEX(product_type_id)

AttributeDefinition

INDEX(product_type_id)

ProductAttributeValue

INDEX(product_id, attribute_id)
INDEX(attribute_id)

---

# Example Product

Product

Paracetamol 500mg

Product Type

Drug

Attributes

- dosage: 500mg
- manufacturer: Example Pharma
- prescription_required: false
- expiry_date: 2026-05-01

---

# Future Extensions

Possible improvements:

Product variants

Example:

- Size
- Color

Inventory management

Multi-warehouse support

Search engine integration

For large catalogs, integrating Elasticsearch or OpenSearch can significantly improve filtering and attribute search performance.

---

# Summary

This design provides a flexible and scalable structure for managing multiple product types with dynamic attributes while maintaining a clean relational database model suitable for production systems.


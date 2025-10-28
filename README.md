# Online Library System

Distributed Java application with library management, member clients, and book suppliers using multiple network protocols.

## Components

**Library Server (GUI)**
- Member management (CRUD, approval, blocking) - XML storage
- Book management - Redis database
- REST API for member communication
- Socket server for supplier orders

**Member Client (GUI)**
- Registration/login via REST
- Book browsing with preview (cover + 100 lines)
- Multi-book download as ZIP via email
- Secure chat (SSL sockets)
- Multicast for book recommendations

**Supplier Client (GUI)**
- Auto-import books from Project Gutenberg
- Socket communication with library
- Order processing (FIFO from message queue)
- RMI-based invoicing with accounting service

## Network Protocols

- REST API (library ↔ members)
- TCP sockets (library ↔ suppliers)
- SSL sockets (member chat)
- Multicast (recommendations)
- Message Queue (orders)
- RMI (invoicing)

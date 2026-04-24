  CREATE TABLE users (
      id BIGSERIAL PRIMARY KEY,
      email VARCHAR(255) UNIQUE NOT NULL,
      password VARCHAR(255) NOT NULL,
      full_name VARCHAR(255),
      wallet_balance DECIMAL(19, 4) DEFAULT 0.0,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP
  );

  CREATE TABLE auctions (
      id BIGSERIAL PRIMARY KEY,
      title VARCHAR(255) NOT NULL,
      description TEXT,
      starting_price DECIMAL(19, 4) NOT NULL,
      end_time TIMESTAMP NOT NULL,
      status VARCHAR(50) NOT NULL,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP
  );
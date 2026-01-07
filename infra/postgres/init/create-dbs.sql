-- Runs only on first initialization of the postgres data directory.
CREATE DATABASE bookswap_catalog_db;
CREATE DATABASE bookswap_email_db;
CREATE DATABASE bookswap_media_db;
CREATE DATABASE bookswap_notification_db;
CREATE DATABASE bookswap_swap_db;
CREATE DATABASE bookswap_valuation_db;
CREATE DATABASE bookswap_wallet_db;

-- Ensure the dev user owns them (POSTGRES_USER = bookswap)
ALTER DATABASE bookswap_catalog_db OWNER TO bookswap;
ALTER DATABASE bookswap_email_db OWNER TO bookswap;
ALTER DATABASE bookswap_media_db OWNER TO bookswap;
ALTER DATABASE bookswap_notification_db OWNER TO bookswap;
ALTER DATABASE bookswap_swap_db OWNER TO bookswap;
ALTER DATABASE bookswap_valuation_db OWNER TO bookswap;
ALTER DATABASE bookswap_wallet_db OWNER TO bookswap;

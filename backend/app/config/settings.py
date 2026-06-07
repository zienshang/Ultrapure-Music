from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    app_name: str = "UltrapureMusic"
    debug: bool = True
    secret_key: str = "change-this-to-a-random-secret-key-in-production"
    allowed_origins: str = "*"

    database_url: str = "sqlite:///./data/ultrapure.db"

    google_client_id: str = ""
    google_client_secret: str = ""
    google_redirect_uri: str = "http://localhost:8000/api/v1/auth/google/callback"

    youtube_api_key: str = ""

    jwt_algorithm: str = "HS256"
    jwt_expiration_minutes: int = 1440

    cache_max_age_hours: int = 24
    cache_cleanup_interval_hours: int = 6

    sync_interval_hours: int = 12


settings = Settings()

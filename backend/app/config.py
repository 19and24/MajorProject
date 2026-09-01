from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy import URL

class Settings(BaseSettings):
    DATABASE_HOST: str
    DATABASE_PORT: int = 4000
    DATABASE_NAME: str
    DATABASE_USER: str
    DATABASE_PASSWORD: str
    DATABASE_SSL_CA: str | None = None

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
    )

    @property
    def database_url(self) -> URL:
        query = {
            "ssl_verify_cert": "true",
            "ssl_verify_identity": "true",
        }

        if self.DATABASE_SSL_CA:
            query["ssl_ca"] = self.DATABASE_SSL_CA

        return URL.create(
            drivername="mysql+pymysql",
            username=self.DATABASE_USER,
            password=self.DATABASE_PASSWORD,
            host=self.DATABASE_HOST,
            port=self.DATABASE_PORT,
            database=self.DATABASE_NAME,
            query=query,
        )

    @property
    def database_url_string(self) -> str:
        return self.database_url.render_as_string(
            hide_password=False,
        )
settings = Settings() # type: ignore
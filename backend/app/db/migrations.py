from pathlib import Path

from app.db.database import get_connection, commit
from app.utils.logger import get_logger

logger = get_logger(__name__)


async def run_migrations() -> None:
    schema_path = Path(__file__).parent / "schema.sql"
    if not schema_path.exists():
        logger.error("schema.sql not found at %s", schema_path)
        return

    schema_sql = schema_path.read_text(encoding="utf-8")
    db = await get_connection()
    await db.executescript(schema_sql)
    await commit()
    logger.info("Database migrations applied successfully")

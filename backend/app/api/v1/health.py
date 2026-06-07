from fastapi import APIRouter

from app.config.constants import APP_VERSION
from app.db.database import execute

router = APIRouter()


@router.get("/health")
async def health_check():
    db_ok = False
    try:
        await execute("SELECT 1")
        db_ok = True
    except Exception:
        db_ok = False
    return {
        "status": "healthy",
        "version": APP_VERSION,
        "database": "connected" if db_ok else "error",
    }

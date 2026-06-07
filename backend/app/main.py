from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.router import api_router
from app.config.constants import APP_VERSION, PROJECT_DESCRIPTION
from app.config.settings import settings
from app.db.database import close_connection
from app.db.migrations import run_migrations
from app.tasks.scheduler import start_scheduler, stop_scheduler
from app.utils.logger import setup_logging
from app.utils.exceptions import (
    AppException,
    app_exception_handler,
    generic_exception_handler,
)
from app.utils.rate_limiter import RateLimitMiddleware


@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_logging()
    await run_migrations()
    await start_scheduler()
    yield
    await stop_scheduler()
    await close_connection()


app = FastAPI(
    title=settings.app_name,
    description=PROJECT_DESCRIPTION,
    version=APP_VERSION,
    lifespan=lifespan,
)

app.add_exception_handler(AppException, app_exception_handler)
app.add_exception_handler(Exception, generic_exception_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins.split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

if not settings.debug:
    app.add_middleware(RateLimitMiddleware, max_requests=60, window_seconds=60)

app.include_router(api_router)


@app.get("/", tags=["Health"])
async def root():
    return {"app": settings.app_name, "version": APP_VERSION, "status": "running"}

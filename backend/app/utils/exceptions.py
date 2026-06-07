from fastapi import HTTPException, Request
from fastapi.responses import JSONResponse

from app.config.settings import settings


class AppException(Exception):
    def __init__(self, message: str, status_code: int = 400, detail: str = None):
        self.message = message
        self.status_code = status_code
        self.detail = detail


class NotFoundException(AppException):
    def __init__(self, message: str = "Resource not found", detail: str = None):
        super().__init__(message=message, status_code=404, detail=detail)


class UnauthorizedException(AppException):
    def __init__(self, message: str = "Unauthorized", detail: str = None):
        super().__init__(message=message, status_code=401, detail=detail)


class ForbiddenException(AppException):
    def __init__(self, message: str = "Forbidden", detail: str = None):
        super().__init__(message=message, status_code=403, detail=detail)


class ConflictException(AppException):
    def __init__(self, message: str = "Conflict", detail: str = None):
        super().__init__(message=message, status_code=409, detail=detail)


class ValidationException(AppException):
    def __init__(self, message: str = "Validation error", detail: str = None):
        super().__init__(message=message, status_code=422, detail=detail)


class ServiceUnavailableException(AppException):
    def __init__(self, message: str = "Service unavailable", detail: str = None):
        super().__init__(message=message, status_code=503, detail=detail)


async def app_exception_handler(request: Request, exc: AppException):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": exc.message,
            "detail": exc.detail,
            "status_code": exc.status_code,
        },
    )


async def generic_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "detail": str(exc) if settings.debug else None,
            "status_code": 500,
        },
    )

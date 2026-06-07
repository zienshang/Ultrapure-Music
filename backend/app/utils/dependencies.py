from fastapi import Header, HTTPException, Depends

from app.db.repositories.users import UserRepository
from app.utils.token_utils import verify_access_token


async def get_current_user_id(authorization: str = Header(None)) -> int:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")
    token = authorization.replace("Bearer ", "")
    payload = verify_access_token(token)
    if not payload:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    return payload["user_id"]


async def get_optional_user_id(authorization: str = Header(None)) -> int | None:
    if not authorization or not authorization.startswith("Bearer "):
        return None
    token = authorization.replace("Bearer ", "")
    payload = verify_access_token(token)
    if not payload:
        return None
    return payload["user_id"]


async def get_current_user(authorization: str = Header(None)) -> dict:
    user_id = await get_current_user_id(authorization)
    user = await UserRepository.get_by_id(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user

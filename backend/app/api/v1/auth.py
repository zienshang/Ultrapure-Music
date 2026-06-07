from fastapi import APIRouter, Query, HTTPException, Header

from app.schemas.auth import GoogleAuthRequest, TokenResponse, UserResponse
from app.services.auth_service import auth_service
from app.utils.exceptions import UnauthorizedException

router = APIRouter()


@router.get("/google/url")
async def get_google_auth_url():
    from google_auth_oauthlib.flow import Flow
    from app.config.settings import settings
    flow = Flow.from_client_config(
        client_config={
            "web": {
                "client_id": settings.google_client_id,
                "client_secret": settings.google_client_secret,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "redirect_uris": [settings.google_redirect_uri],
            }
        },
        scopes=[
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/youtube.readonly",
        ],
        redirect_uri=settings.google_redirect_uri,
    )
    url, _ = flow.authorization_url(prompt="consent")
    return {"auth_url": url}


@router.post("/google/callback", response_model=TokenResponse)
async def google_callback(body: GoogleAuthRequest):
    result = await auth_service.google_login(body.code)
    if not result:
        raise HTTPException(status_code=401, detail="Google authentication failed")
    return TokenResponse(**result)


@router.get("/me", response_model=UserResponse)
async def get_current_user(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid token")
    token = authorization.replace("Bearer ", "")
    payload = auth_service.get_current_user_from_token(token)
    if not payload:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    user = await auth_service.get_user_profile(payload["user_id"])
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return UserResponse(**user)


@router.post("/logout")
async def logout(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid token")
    token = authorization.replace("Bearer ", "")
    payload = auth_service.get_current_user_from_token(token)
    if payload:
        auth_service.logout(payload["user_id"])
    return {"status": "logged_out"}

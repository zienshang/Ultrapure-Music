from google.oauth2.credentials import Credentials
from google.auth.transport.requests import Request
from googleapiclient.discovery import build

from app.config.settings import settings
from app.db.repositories.users import UserRepository
from app.utils.token_utils import create_access_token, verify_access_token
from app.utils.logger import get_logger

logger = get_logger(__name__)


class AuthService:
    _refresh_tokens: dict[int, str] = {}

    @staticmethod
    async def google_login(code: str) -> dict | None:
        try:
            from google_auth_oauthlib.flow import Flow
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
            flow.fetch_token(code=code)
            credentials = flow.credentials

            service = build("oauth2", "v2", credentials=credentials)
            user_info = service.userinfo().get().execute()

            email = user_info.get("email", "")
            name = user_info.get("name", email.split("@")[0])
            google_id = user_info.get("id", "")
            avatar = user_info.get("picture", "")

            user = await UserRepository.find_or_create(
                email=email,
                display_name=name,
                google_id=google_id,
                avatar_url=avatar,
            )

            access_token = create_access_token(user["id"], email)
            AuthService._refresh_tokens[user["id"]] = credentials.refresh_token or ""

            return {
                "access_token": access_token,
                "token_type": "bearer",
                "user": user,
            }
        except Exception as e:
            logger.error("Google login failed: %s", str(e))
            return None

    @staticmethod
    def get_current_user_from_token(token: str) -> dict | None:
        payload = verify_access_token(token)
        if not payload:
            return None
        return payload

    @staticmethod
    async def get_user_profile(user_id: int) -> dict | None:
        return await UserRepository.get_by_id(user_id)

    @staticmethod
    def logout(user_id: int) -> None:
        AuthService._refresh_tokens.pop(user_id, None)
        logger.info("User %s logged out", user_id)


auth_service = AuthService()

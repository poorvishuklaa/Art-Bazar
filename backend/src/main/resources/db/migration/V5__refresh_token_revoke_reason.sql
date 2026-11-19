-- Why a refresh token stopped being valid. Only ROTATED tokens get the short reuse grace period;
-- tokens revoked by logout, password change, account disabling or theft detection never come back.
ALTER TABLE refresh_tokens ADD COLUMN revoke_reason VARCHAR(20) NULL;

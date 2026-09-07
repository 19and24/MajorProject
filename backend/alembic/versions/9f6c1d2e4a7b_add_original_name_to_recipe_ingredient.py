"""add original name to recipe ingredient

Revision ID: 9f6c1d2e4a7b
Revises: 15b29cde8c84
Create Date: 2026-09-02

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "9f6c1d2e4a7b"
down_revision: Union[str, Sequence[str], None] = "15b29cde8c84"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "recipe_ingredient",
        sa.Column("original_name", sa.String(length=100), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("recipe_ingredient", "original_name")

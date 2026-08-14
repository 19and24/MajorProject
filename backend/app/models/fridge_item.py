from sqlalchemy import DECIMAL, String, DateTime, Boolean, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from decimal import Decimal
from datetime import datetime
from app.database import Base


class FridgeItem(Base):
    __tablename__ = "fridge_item"

    id:Mapped[int] = mapped_column(
        primary_key=True,
        autoincrement=True,
    )

    ingredient_id: Mapped[int] = mapped_column(
        ForeignKey("ingredient.id"),
        nullable=False,
    )

    quantity: Mapped[Decimal] = mapped_column(
        DECIMAL(10, 2),
        nullable=False,
    )

    unit: Mapped[str] = mapped_column(
        String(30),
        nullable=False,
    )

    deadline: Mapped[datetime | None] = mapped_column(
        DateTime,
        nullable=False,
    )

    starred: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=datetime.now,
        nullable=False,
    )

    updated_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=datetime.now,
        onupdate=datetime.now,
        nullable=False,
    )

    ingredient = relationship("Ingredient", back_populates="fridge_items")
    notifications = relationship(
        "Notification",
        back_populates="fridge_item",
        cascade="all, delete-orphan",
        passive_deletes=True,
    )

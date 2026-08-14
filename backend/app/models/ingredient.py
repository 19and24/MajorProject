from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Ingredient(Base):
    __tablename__ = "ingredient"

    id: Mapped[int] = mapped_column(
        primary_key=True,
        autoincrement=True,
    )

    name: Mapped[str] = mapped_column(
        String(100),
        nullable=False,
        unique=True,
    )

    category: Mapped[str | None] = mapped_column(
        String(120),
    )

    emoji: Mapped[str | None] = mapped_column(
        String(20),
    )

    fridge_items = relationship("FridgeItem", back_populates="ingredient")

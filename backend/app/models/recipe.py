from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Recipe(Base):
    __tablename__ = "recipe"

    id: Mapped[int] = mapped_column(
        primary_key=True,
        autoincrement=True,
    )

    title: Mapped[str] = mapped_column(
        String(100),
        nullable=False,
    )

    method: Mapped[str] = mapped_column(
        String(200),
    )

    ingredient_acto: Mapped[str] = mapped_column(
        String(200),
    )

    type: Mapped[str] = mapped_column(
        String(200),
    )

    ingredient_all: Mapped[str] = mapped_column(
        String(400),
    )

    portion: Mapped[str] = mapped_column(
        String(20),
    )

    cooktime: Mapped[str] = mapped_column(
        String(20),
    )

    recipe_ingredients = relationship(
        "RecipeIngredient",
        back_populates="recipe",
        cascade="all, delete-orphan",
    )
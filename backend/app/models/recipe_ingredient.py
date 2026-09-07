from sqlalchemy import Boolean, ForeignKey, PrimaryKeyConstraint, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class RecipeIngredient(Base):
    __tablename__ = "recipe_ingredient"

    recipe_id: Mapped[int] = mapped_column(
        ForeignKey("recipe.id"),
        nullable=False,
    )

    ingredient_id: Mapped[int] = mapped_column(
        ForeignKey("ingredient.id"),
        nullable=False,
        index=True,
    )

    amount: Mapped[str | None] = mapped_column(
        String(50),
    )

    original_name: Mapped[str | None] = mapped_column(
        String(100),
    )

    optional: Mapped[bool] = mapped_column(
        Boolean,
        default=False,
    )

    __table_args__ = (
        PrimaryKeyConstraint("recipe_id", "ingredient_id"),
    )

    recipe = relationship("Recipe", back_populates="recipe_ingredients")
    ingredient = relationship("Ingredient")

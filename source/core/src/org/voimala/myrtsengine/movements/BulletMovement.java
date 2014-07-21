package org.voimala.myrtsengine.movements;

import org.voimala.myrtsengine.screens.gameplay.world.AbstractGameObject;

public class BulletMovement extends AbstractMovement {

    public BulletMovement(final AbstractGameObject owner) {
        super(owner);
    }

    @Override
    public void update(float deltaTime) {
        handlePhysicalMotion(deltaTime);
    }

    private void handlePhysicalMotion(float deltaTime) {
        handleVelocity(deltaTime);
    }

    private void handleVelocity(float deltaTime) { // TODO Makes turret go crazy??
        owner.moveX(Math.cos(owner.getAngleInRadians()) * currentVelocity * deltaTime);
        owner.moveY(Math.sin(owner.getAngleInRadians()) * currentVelocity * deltaTime);
    }

}
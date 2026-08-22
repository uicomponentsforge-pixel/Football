/** 2D Vector Math Helper Class */
class Vec2 {
    constructor(x = 0, y = 0) {
        this.x = x;
        this.y = y;
    }

    plus(other) {
        return new Vec2(this.x + other.x, this.y + other.y);
    }

    minus(other) {
        return new Vec2(this.x - other.x, this.y - other.y);
    }

    times(scalar) {
        return new Vec2(this.x * scalar, this.y * scalar);
    }

    div(scalar) {
        return scalar !== 0 ? new Vec2(this.x / scalar, this.y / scalar) : new Vec2(0, 0);
    }

    length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    distanceTo(other) {
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    normalized() {
        const len = this.length();
        return len > 0.0001 ? new Vec2(this.x / len, this.y / len) : new Vec2(0, 0);
    }

    angle() {
        return Math.atan2(this.y, this.x);
    }

    static zero() {
        return new Vec2(0, 0);
    }

    static fromAngle(angleRad, length = 1) {
        return new Vec2(Math.cos(angleRad) * length, Math.sin(angleRad) * length);
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = Vec2;
}

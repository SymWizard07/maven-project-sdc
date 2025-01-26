package com.whitley.cade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;

public class Main extends JPanel {
    private World world;
    private Timer timer;
    private JFrame frame;
    private int lastFrameX, lastFrameY;

    private static final float SCALE = 30.0f;

    public Main() {
        frame = new JFrame("Drag Me!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // Track the initial frame position
        Point frameLocation = frame.getLocationOnScreen();
        lastFrameX = frameLocation.x;
        lastFrameY = frameLocation.y;

        trackWindowPosition();
        initPhysics();
        startSimulation();
    }

    private void trackWindowPosition() {
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                Point newLocation = frame.getLocationOnScreen();
                int newX = newLocation.x;
                int newY = newLocation.y;

                // Calculate movement delta in pixels and scale to physics units
                float dx = (float) (newX - lastFrameX) / (SCALE);
                float dy = (float) (newY - lastFrameY) / (SCALE);

                // Move the boundaries with the window
                for (Body body = world.getBodyList(); body != null; body = body.getNext()) {
                    if (body.getType() == BodyType.STATIC) {
                        moveBoundary(body, dx / SCALE, dy / SCALE);
                    } else {
                        // Move dynamic objects in the opposite direction to appear stationary
                        moveBody(body, -dx, dy);
                    }
                }

                lastFrameX = newX;
                lastFrameY = newY;
            }
        });
    }

    private void moveBoundary(Body body, float dx, float dy) {
        // Calculate velocity needed to match window movement instantly
        float vx = dx / (1.0f / 60.0f);
        float vy = -dy / (1.0f / 60.0f);
        body.setLinearVelocity(new Vec2(vx, vy));
    }
    

    private void moveBody(Body body, float dx, float dy) {
        Vec2 pos = body.getPosition();
        body.setTransform(new Vec2(pos.x + dx, pos.y + dy), body.getAngle());
    }

    private void initPhysics() {
        Vec2 gravity = new Vec2(0.0f, -30.0f);
        world = new World(gravity);

        // Get the dimensions of the content pane to align the boundaries
        int contentWidth = frame.getContentPane().getWidth();
        int contentHeight = frame.getContentPane().getHeight();
        float halfWidth = contentWidth / (2.0f * SCALE);
        float halfHeight = contentHeight / (2.0f * SCALE);

        // Create boundaries (floor, walls, ceiling)
        createBoundary(0, halfHeight * 1.2f, halfWidth, 3.0f);  // Floor
        createBoundary(0, -halfHeight * 1.2f, halfWidth, 3.0f); // Ceiling
        createBoundary(-halfWidth * 1.2f, 0, 3.0f, halfHeight); // Left wall
        createBoundary(halfWidth * 1.2f, 0, 3.0f, halfHeight); // Right wall

        // Add dynamic shapes inside the world
        createBox(0, 2);
        createCircle(-3, 5);
        createTriangle(3, 7);
    }

    private void createBoundary(float x, float y, float halfWidth, float halfHeight) {
        BodyDef boundaryDef = new BodyDef();
        boundaryDef.position.set(x, y);
        Body boundaryBody = world.createBody(boundaryDef);
        PolygonShape boundaryShape = new PolygonShape();
        boundaryShape.setAsBox(halfWidth, halfHeight);
        boundaryBody.createFixture(boundaryShape, 0.0f);
        boundaryBody.getFixtureList().setRestitution(0.5f);
    }

    private void createBox(float x, float y) {
        BodyDef boxDef = new BodyDef();
        boxDef.type = BodyType.DYNAMIC;
        boxDef.position.set(x, y);
        Body boxBody = world.createBody(boxDef);
        PolygonShape boxShape = new PolygonShape();
        boxShape.setAsBox(3.0f, 3.0f);
        boxBody.createFixture(boxShape, 1.0f);
        boxBody.getFixtureList().setFriction(0.5f);
        boxBody.getFixtureList().setRestitution(0.5f);
        boxBody.setSleepingAllowed(false);
    }

    private void createCircle(float x, float y) {
        BodyDef circleDef = new BodyDef();
        circleDef.type = BodyType.DYNAMIC;
        circleDef.position.set(x, y);
        Body circleBody = world.createBody(circleDef);
        CircleShape circleShape = new CircleShape();
        circleShape.m_radius = 2.0f;
        circleBody.createFixture(circleShape, 1.0f);
        circleBody.getFixtureList().setRestitution(0.5f);
        circleBody.setSleepingAllowed(false);
    }

    private void createTriangle(float x, float y) {
        BodyDef triangleDef = new BodyDef();
        triangleDef.type = BodyType.DYNAMIC;
        triangleDef.position.set(x, y);
        Body triangleBody = world.createBody(triangleDef);
        PolygonShape triangleShape = new PolygonShape();
        Vec2[] vertices = new Vec2[3];
        vertices[0] = new Vec2(0.0f, 0.0f);
        vertices[1] = new Vec2(3.0f, 0.0f);
        vertices[2] = new Vec2(0.0f, 3.0f);
        triangleShape.set(vertices, 3);
        triangleBody.createFixture(triangleShape, 1.0f);
        triangleBody.getFixtureList().setRestitution(0.5f);
        triangleBody.setSleepingAllowed(false);
    }

    private void startSimulation() {
        timer = new Timer(16, e -> {
            world.step(1.0f / 60.0f, 12, 6);
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());

        renderBodies(g);
    }

    public void renderBodies(Graphics g) {
        for (Body body = world.getBodyList(); body != null; body = body.getNext()) {
            Vec2 position = body.getPosition();
            int screenX = (int) (position.x * SCALE + getWidth() / 2);
            int screenY = (int) (-position.y * SCALE + getHeight() / 2);

            for (Fixture fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
                Shape shape = fixture.getShape();

                if (shape.getType() == ShapeType.POLYGON) {
                    PolygonShape polygon = (PolygonShape) shape;
                    int vertexCount = polygon.getVertexCount();
                    int[] xPoints = new int[vertexCount];
                    int[] yPoints = new int[vertexCount];

                    for (int i = 0; i < vertexCount; i++) {
                        Vec2 worldVertex = body.getWorldPoint(polygon.getVertex(i));
                        xPoints[i] = (int) (worldVertex.x * SCALE + getWidth() / 2);
                        yPoints[i] = (int) (-worldVertex.y * SCALE + getHeight() / 2);
                    }

                    g.setColor(Color.YELLOW);
                    if (vertexCount == 3) {
                        g.setColor(Color.BLUE);
                    }
                    else if (vertexCount == 4) {
                        g.setColor(Color.GREEN);
                    }
                    if (body.getType() == BodyType.STATIC) {
                        g.setColor(Color.WHITE);
                    }

                    g.fillPolygon(xPoints, yPoints, vertexCount);
                } else if (shape.getType() == ShapeType.CIRCLE) {
                    CircleShape circle = (CircleShape) shape;
                    float radius = circle.m_radius * SCALE;
                    g.setColor(Color.RED);
                    g.fillOval(screenX - (int) radius, screenY - (int) radius, (int) (2 * radius), (int) (2 * radius));
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}

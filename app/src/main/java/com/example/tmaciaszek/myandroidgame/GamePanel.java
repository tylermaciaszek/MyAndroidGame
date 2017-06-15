package com.example.tmaciaszek.myandroidgame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by tmaci on 6/12/2017.
 */

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {

    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;

    private Background bg;

    private Hero hero;

    private Random rand = new Random();
    private ArrayList<Enemy> enemy;
    private long enemyStartTime;

    private ArrayList<Obstacle> obstacle;
    private long obstacleStartTime;

    Bitmap coinbonus;
    public int herocoins;

    private long bonusStartTime;
    private ArrayList<Bonus> mycoins;

    private boolean newGameCreated;
    private long startReset;
    private boolean reset;
    private boolean dissapear;
    private boolean started;


    //Thread
    private MainThread thread;

    //Constructor
    public GamePanel(Context context){
        super(context);


        getHolder().addCallback(this);

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new MainThread(getHolder(), this);

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.background));
        hero = new Hero(BitmapFactory.decodeResource(getResources(), R.drawable.hero), 55, 54, 8);

        enemy = new ArrayList<Enemy>();
        enemyStartTime = System.nanoTime();

        obstacle = new ArrayList<Obstacle>();
        obstacleStartTime = System.nanoTime();

        mycoins = new ArrayList<Bonus>();
        bonusStartTime = System.nanoTime();

        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;


        while(retry){
            try{
                thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(!hero.getPlaying()){
                hero.setPlaying(true);
            }else{
                hero.setUp(true);
                }
            return true;
            }
        if(event.getAction() == MotionEvent.ACTION_UP){
            hero.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update(){

        if(hero.getPlaying()) {
            bg.update();
            hero.update();

             if(hero.getY() < 0 || hero.getY() > GamePanel.HEIGHT - 40){
                        newGame();
                        hero.setPlaying(false);
                    }

            long bonustime = (System.nanoTime() - bonusStartTime) /1000000;
            if((bonustime > (3000 - hero.getScore()/4))) {
                mycoins.add(new Bonus((BitmapFactory.decodeResource(getResources(), R.drawable.bonus)),
                        WIDTH + 1, (int) (rand.nextDouble() * (HEIGHT - 200)), 40, 40, 4));
                bonusStartTime = System.nanoTime();
            }
            long obstacleElapsed = (System.nanoTime() - obstacleStartTime) / 1000000;

            if(obstacleElapsed > (3500 - hero.getScore()/4)){
                obstacle.add(new Obstacle(BitmapFactory.decodeResource(getResources(), R.drawable.obstacle),
                        WIDTH + 10, HEIGHT - 229 + rand.nextInt(150), 90, 229, hero.getScore(), 1));
                obstacleStartTime = System.nanoTime();
            }

            for(int i = 0; i < mycoins.size(); i++){
                mycoins.get(i).update();
                if(collision(mycoins.get(i),hero)){
                    mycoins.remove(i);
                    hero.setScore(hero.getScore()+ 5);
                    System.out.println(hero.getScore());
                    break;
                }

                if(mycoins.get(i).getX() <-100){
                    mycoins.remove(i);
                    break;
                }
            }

            for(int i = 0; i < obstacle.size(); i++){
                obstacle.get(i).update();

                if(collision(obstacle.get(i), hero)){
                    hero.setPlaying(false);
                    newGame();
                    break;
                }

                if(obstacle.get(i).getX() < -100){
                    obstacle.remove(i);
                    break;
                }
            }

            long enemyElapsed = (System.nanoTime() - enemyStartTime) / 1000000;
            if(enemyElapsed > (3000 - hero.getScore() / 4)){



                enemy.add(new Enemy(BitmapFactory.decodeResource(getResources(), R.drawable.enemy),
                        WIDTH + 10, (int) (rand.nextDouble() * (HEIGHT - 50)), 50, 42, hero.getScore(), 1));
                enemyStartTime = System.nanoTime();
            }

            for(int i = 0; i < enemy.size(); i++){
                enemy.get(i).update();

                if(collision(enemy.get(i), hero)){
                    enemy.remove(i);
                    hero.setPlaying(false);
                    newGame();
                    break;
                }

                if(enemy.get(i).getX() < -100){
                    enemy.remove(i);
                    break;
                }
            }
        }else{

            hero.resetDYA();
            if(!reset){
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                dissapear = true;
            }

            long resetElapsed = (System.nanoTime() - startReset) / 1000000;

            if(resetElapsed > 2500 && !newGameCreated){
                newGame();
            }
        }
    }

    public boolean collision(GameObject a, GameObject b){
        if(Rect.intersects(a.getRectangle(), b.getRectangle())){
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas){
        super.draw(canvas);
        final float scaleFactorX = getWidth()/(WIDTH*1.f);
        final float scaleFactorY = getHeight()/(HEIGHT*1.f);

        if (canvas != null){
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            if(!dissapear) {
                hero.draw(canvas);
            }
            //bb.draw(canvas);
            //bt.draw(canvas);

            for(Enemy en: enemy){
                en.draw(canvas);
            }

            for(Obstacle obs: obstacle){
                obs.draw(canvas);
            }

            for(Bonus co: mycoins){
                co.draw(canvas);
            }


            canvas.restoreToCount(savedState);
        }
    }

    public void newGame(){
        dissapear = false;

        enemy.clear();
        obstacle.clear();
        mycoins.clear();

        hero.resetScore();
        hero.resetDYA();

        hero.setY(HEIGHT/2);

        newGameCreated = true;

    }
}






















































































































































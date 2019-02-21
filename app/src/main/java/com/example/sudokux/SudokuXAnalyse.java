package com.example.sudokux;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Stack;

public class SudokuXAnalyse {
    /*数独二维数组*/
    private int[][] mShuDu = new int[9][9];
    /*二维数组，标记某个格子是否被修改过，初始化全为false，填入数字后置为true*/
    private boolean[][] mShuDuFlag = new boolean[9][9];

    public SudokuXAnalyse(int[][] shuDu) {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                mShuDu[i][j] = shuDu[i][j];
                mShuDuFlag[i][j] = false;
            }
        }
    }

    /*得到某个格子可能填入的数字序列*/
    private  ArrayList<Integer> getPendingQueue(int x, int y)  {
        int tmp[] = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < 9; i++) {
            if (mShuDu[x][i] != 0 && i != y) {
                tmp[mShuDu[x][i] - 1] = -1;
            }
            if (mShuDu[i][y] != 0 && i != x) {
                tmp[mShuDu[i][y] - 1] = -1;
            }
        }
        for (int i = x / 3 * 3; i < x / 3 * 3 + 3; i++) {
            for (int j = y / 3 * 3; j < y / 3 * 3 + 3; j++) {
                if (i == x && j == y) {
                    continue;
                }
                if (mShuDu[i][j] != 0) {
                    tmp[mShuDu[i][j] - 1] = -1;
                }
            }
        }

        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (tmp[i] != -1) {
                arrayList.add(Integer.valueOf(tmp[i]));
            }
        }
        return arrayList;
    }

    /*把坐标（beginX,beginY）到（endX,endY）全部被修改过的格子置为0，在回溯时使用*/
    private void clear(int beginX, int beginY, int endX, int endY) {
        int end = endX * 9 + endY;
        int begin = beginX * 9 + beginY;

        while (end > begin) {
            if (mShuDuFlag[end / 9][end % 9]) {
                mShuDu[end / 9][end % 9] = 0;
            }
            end--;
        }
    }

    /*数独求解，无解时返回null*/
    public int[][] getAns() throws InterruptedException {
        int i = 0, j = 0;
        boolean needContinue = true;
        /*栈中存放键值对，key为某格子的下标，value为该格子可能填入数字的序列*/
        Stack<Pair<String, ArrayList<Integer>>> stack = new Stack<>();

        while (needContinue) {
            needContinue = false;
            while (i < 9) {
                while (j < 9) {
                    if (mShuDu[i][j] == 0) {
                        needContinue = true;
                        ArrayList<Integer> arrayList = getPendingQueue(i, j);
                        //当某格子没有可以填入的数字时，回溯
                        if (arrayList.size() == 0) {
                            //栈空，无解
                            if (stack.size() == 0) {
                                return null;
                            }
                            int tmpI = stack.peek().first.charAt(0) - '0';
                            int tmpJ = stack.peek().first.charAt(1) - '0';

                            clear(tmpI, tmpJ, i, j);

                            //重新更新当前下标
                            i = tmpI;
                            j = tmpJ;

                            //填入某格子的下一个可能数字
                            mShuDu[i][j] = stack.peek().second.remove(0);

                            if (stack.peek().second.size() == 0) {
                                stack.pop();
                            }
                        } else {
                            mShuDu[i][j] = arrayList.remove(0);
                            mShuDuFlag[i][j] = true;
                            //保存某格子可能填入的其余数字
                            if (!arrayList.isEmpty()) {
                                String key = i + "" + j;
                                Pair<String, ArrayList<Integer>> pair = new Pair<>(key, arrayList);
                                stack.push(pair);
                            }
                        }
                    }
                    j++;
                }
                i++;
                j = 0;
            }
        }
        return mShuDu;
    }
}

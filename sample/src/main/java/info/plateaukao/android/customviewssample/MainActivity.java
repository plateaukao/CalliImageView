package info.plateaukao.android.customviewssample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import info.plateaukao.android.customviews.CalliImageView;

public class MainActivity extends AppCompatActivity {
    CalliImageView civ;
    Button btStyle, btGrid, btChar;
    boolean isCharShown = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUi();
    }

    private void setupUi() {
        civ = (CalliImageView) findViewById(R.id.char_imageview);
        civ.setImageDrawable(getResources().getDrawable(R.drawable.sample));

        btStyle= (Button) findViewById(R.id.btStyle);
        btStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                civ.setDrawType(
                    (CalliImageView.DRAW_TYPE.NORMAL == civ.getDrawType())
                        ?CalliImageView.DRAW_TYPE.CONTOUR
                        :CalliImageView.DRAW_TYPE.NORMAL);
            }
        });

        btGrid = (Button) findViewById(R.id.btGrid);
        btGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                civ.setGridType(
                    (CalliImageView.GRID_TYPE.GRID_9 == civ.getGridType())
                        ?CalliImageView.GRID_TYPE.DIAGNAL
                        :CalliImageView.GRID_TYPE.GRID_9);
            }
        });

        btChar = (Button) findViewById(R.id.btChar);
        btChar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCharShown = !isCharShown;
                civ.setIsShowChar(isCharShown);
            }
        });
    }
}

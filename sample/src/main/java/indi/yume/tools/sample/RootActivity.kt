package indi.yume.tools.sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class RootActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)

        findViewById<View>(R.id.compose_demo_btn).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<View>(R.id.tree_demo_btn).setOnClickListener {
            startActivity(Intent(this, TreeActivity::class.java))
        }
        findViewById<View>(R.id.drop_demo_btn).setOnClickListener {
            startActivity(Intent(this, DragActivity::class.java))
        }
        findViewById<View>(R.id.list_paging_demo_btn).setOnClickListener {
            startActivity(Intent(this, ListPagingActivity::class.java))
        }
    }
}

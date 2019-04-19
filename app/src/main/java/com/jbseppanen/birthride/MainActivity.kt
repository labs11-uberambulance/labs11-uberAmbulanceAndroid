package com.jbseppanen.birthride

import android.content.Context
import android.content.Intent
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


open class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var context: Context

    protected fun onCreateDrawer() {

        context = this

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        val mDrawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerLayout.addDrawerListener(toggle)
//        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

/*    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
       menuInflater.inflate(R.menu.main, menu)
        return true
    }*/

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_editprofile -> {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    delay(300)
                    withContext(Dispatchers.Main) {
                        startActivity(Intent( context, EditAccountDetailsActivity::class.java))
                    }
                }

            }
            R.id.nav_logout -> {
                AuthUI.getInstance().signOut(this).addOnCompleteListener {
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()
                }
            }
/*            R.id.nav_deleteaccount -> {
                //For future delete account option.
            }*/

        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}

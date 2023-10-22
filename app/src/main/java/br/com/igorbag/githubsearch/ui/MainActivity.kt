package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.data.NetworkUtils
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

private const val USER_KEY = "user_salvo"

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService
    lateinit var retrofitClient: Retrofit
    lateinit var repositoryAdapter: RepositoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        showUserName()
        setupRetrofit()
        getAllReposByUserName()
    }

    fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
        setupListeners()
    }

    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            val nomeUser = nomeUsuario.text.toString()
            saveUserLocal(nomeUser)
            val callback = githubApi.getAllRepositoriesByUser(nomeUser)
            callback.enqueue(object : Callback<List<Repository>> {
                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Toast.makeText(baseContext, t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(
                    call: Call<List<Repository>>,
                    response: Response<List<Repository>>
                ) {
                    response.body()?.let {
                        setupAdapter(it)
                    }
                }
            })
        }
    }

    private fun saveUserLocal(user: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(USER_KEY, user)
            apply()
        }
    }

    private fun showUserName() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        val currentUser = sharedPref.getString(USER_KEY, null)
        if (currentUser != null) {
            nomeUsuario.setText(currentUser)
        }
    }

    private fun setupRetrofit() {
        retrofitClient = NetworkUtils
            .getRetrofitInstance()
        githubApi = retrofitClient.create(GitHubService::class.java)
    }

    private fun getAllReposByUserName() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        val currentUser = sharedPref.getString(USER_KEY, null)
        if (currentUser != null) {
            val callback = githubApi.getAllRepositoriesByUser(currentUser)
            callback.enqueue(object : Callback<List<Repository>> {
                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Toast.makeText(baseContext, t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(
                    call: Call<List<Repository>>,
                    response: Response<List<Repository>>
                ) {
                    response.body()?.let {
                        setupAdapter(it)
                    }
                }
            })
        }
    }

    fun setupAdapter(list: List<Repository>) {
        repositoryAdapter = RepositoryAdapter(list)
        repositoryAdapter.carItemLister = {
            openBrowser(it.htmlUrl)
        }
        repositoryAdapter.btnShareLister = { repository ->
            shareRepositoryLink(repository.htmlUrl)
        }
        listaRepositories.adapter = repositoryAdapter
    }

    private fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }
    private fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )
    }
}